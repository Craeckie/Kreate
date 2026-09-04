package app.kreate.android.service.innertube

import app.kreate.android.service.NewPipeDownloaderImpl
import com.grack.nanojson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume.assumeNoException
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


/**
 * Live-network regression test for the core "can a song actually be played?"
 * question. It exercises the real stream-resolution path used at runtime against
 * YouTube and asserts:
 *
 *  1. the `ANDROID_VR` client resolves the video to a direct (non-ciphered) audio
 *     stream url, and
 *  2. **some** client in the resolver's fallback chain streams **past the
 *     one-minute mark** — i.e. ranged byte requests at 0s / 60s / 120s and the
 *     tail all succeed.
 *
 * Point (2) is the important one: when YouTube requires a PO token that we don't
 * attach, it serves a ~1-minute teaser then `403`s every later range, which the
 * user sees as "the song starts but stops around 1 minute".
 *
 * It asks the same question the app does, and must therefore accept the same
 * answer. `InnertubeResolvingDataSource` walks `ANDROID_VR` → `IOS` → `ANDROID`
 * (progressive itag 18/22) and a song plays as long as *any* of them serves the
 * whole track, so pinning the assertion to `ANDROID_VR` alone would fail the
 * build over a YouTube-side change the app already survives. Only an exhausted
 * chain means songs genuinely will not play. See the *YouTube stream resolution*
 * section of `CLAUDE.md` and `scripts/vr_probe.py`.
 *
 * It is a live-network test: when the network is unavailable (offline CI), it is
 * **skipped** via JUnit assumptions rather than failing the build.
 */
class SongPlaybackTest {

    private companion object {
        /** A widely-available, non-age-restricted, non-"made for kids" video. */
        const val VIDEO_ID = "dQw4w9WgXcQ"

        const val VR_USER_AGENT =
            "com.google.android.apps.youtube.vr.oculus/1.65.10 " +
            "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

        /** Offsets (seconds into the track) whose byte ranges must be servable. */
        val PROBE_SECONDS = listOf( 0, 60, 120 )

        @Volatile
        var initialised = false
    }

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply {
                // Honour the environment proxy so this works inside sandboxed/CI runners,
                // mirroring the innertube module's test client.
                ( System.getenv( "HTTPS_PROXY" ) ?: System.getenv( "https_proxy" ) )
                    ?.takeIf( String::isNotBlank )
                    ?.let { URI( it ) }
                    ?.let { proxy( Proxy( Proxy.Type.HTTP, InetSocketAddress( it.host, it.port ) ) ) }
            }
            .build()
    }

    @BeforeTest
    fun setUp() {
        if( !initialised ) {
            NewPipe.init( NewPipeDownloaderImpl( http ) )
            initialised = true
        }
    }

    /** Skip (don't fail) when the failure is just a lack of connectivity. */
    private fun <T> overNetwork( block: () -> T ): T =
        try {
            block()
        } catch( e: Exception ) {
            if( e is UnknownHostException || e is ConnectException
                || e is SocketTimeoutException || e is SSLException
                || e.message?.contains( "reCaptcha", ignoreCase = true ) == true
            ) {
                assumeNoException( "network unavailable - skipping live-network test", e )
            }
            throw e
        }

    private fun resolveAudioFormat(): JsonObject {
        val response = overNetwork {
            AndroidVrStreamHelper.getAndroidVrPlayerResponse(
                ContentCountry.DEFAULT, Localization.DEFAULT, VIDEO_ID, "testcpn00001"
            )
        }

        val status = response.getObject( "playabilityStatus" ).getString( "status" )
        assertEquals(
            "OK", status,
            "ANDROID_VR could not play $VIDEO_ID (status=$status, " +
            "reason=${response.getObject( "playabilityStatus" ).getString( "reason" )})"
        )

        val audio = bestAudioFormat( response )
        assertNotNull( audio, "no audio adaptive format returned for $VIDEO_ID" )
        return audio
    }

    /**
     * The ANDROID_VR client resolves the video to a direct (pre-signed) audio url.
     */
    @Test
    fun androidVrResolvesPlayableStream() {
        val audio = resolveAudioFormat()
        assertNotNull(
            audio.getString( "url" ),
            "ANDROID_VR returned a ciphered url (signatureCipher) instead of a direct url"
        )
    }

    /**
     * Some client in the resolver's fallback chain serves byte ranges across the whole
     * track — proving a song plays past the one-minute mark, not just the opening teaser.
     *
     * Fails only when **every** client is exhausted, which is the condition under which the
     * app itself gives up. A single client being teaser-blocked is routine and is exactly
     * what the fallback chain exists to absorb.
     */
    @Test
    fun resolvedStreamPlaysPastOneMinute() {
        val rejections = mutableListOf<String>()

        for( client in chain ) {
            val candidate = client.resolve()
            if( candidate == null ) {
                rejections += "${client.name}: no playable direct url returned"
                continue
            }

            val problem = firstUnservableRange( candidate )
            if( problem == null ) return    // this client streams the whole track

            rejections += "${client.name}: $problem"
        }

        fail(
            "no client in the resolution chain served $VIDEO_ID past the teaser - songs " +
            "genuinely will not play:\n" + rejections.joinToString( "\n" ) { "  - $it" }
        )
    }

    /** A resolved stream url plus what is needed to probe and describe it. */
    private class Candidate(
        val url: String,
        val bytesPerSecond: Long,
        val contentLength: Long,
        val userAgent: String
    )

    /** One rung of the resolver's fallback chain. */
    private inner class Client( val name: String, val resolve: () -> Candidate? )

    /**
     * Mirrors `InnertubeResolvingDataSource`'s order: VR first (pot-free, no cipher), then
     * IOS, then the plain ANDROID client's progressive itag 18/22 muxed format.
     */
    private val chain by lazy {
        listOf(
            Client( "ANDROID_VR" ) {
                // Deliberately does not reuse resolveAudioFormat(): that one asserts, which is
                // right for androidVrResolvesPlayableStream but wrong here, where an UNPLAYABLE
                // VR response must fall through to the next client instead of failing the test.
                val response = overNetwork {
                    AndroidVrStreamHelper.getAndroidVrPlayerResponse(
                        ContentCountry.DEFAULT, Localization.DEFAULT, VIDEO_ID, "testcpn00001"
                    )
                }
                if( response.getObject( "playabilityStatus" ).getString( "status" ) != "OK" )
                    return@Client null

                adaptiveCandidate( bestAudioFormat( response ) ?: return@Client null, VR_USER_AGENT )
            },
            Client( "IOS" ) {
                val response = overNetwork {
                    YoutubeStreamHelper.getIosPlayerResponse(
                        ContentCountry.DEFAULT, Localization.DEFAULT, VIDEO_ID, "testcpn00002", null
                    )
                }
                adaptiveCandidate(
                    bestAudioFormat( response ) ?: return@Client null,
                    YoutubeParsingHelper.getIosUserAgent( Localization.DEFAULT )
                )
            },
            Client( "ANDROID (progressive)" ) {
                val response = overNetwork {
                    AndroidStreamHelper.getAndroidPlayerResponse(
                        ContentCountry.DEFAULT, Localization.DEFAULT, VIDEO_ID, "testcpn00003"
                    )
                }
                // Progressive formats live under `formats`, not `adaptiveFormats`; the
                // resolver prefers itag 18, then 22, then whatever carries a url.
                val progressive = response.getObject( "streamingData" )
                    .getArray( "formats" )
                    .filterIsInstance<JsonObject>()
                    .filter { !it.getString( "url" ).isNullOrBlank() }
                val format = progressive.firstOrNull { it.getInt( "itag", 0 ) == 18 }
                    ?: progressive.firstOrNull { it.getInt( "itag", 0 ) == 22 }
                    ?: progressive.firstOrNull()
                    ?: return@Client null
                adaptiveCandidate(
                    format,
                    YoutubeParsingHelper.getAndroidUserAgent( Localization.DEFAULT )
                )
            }
        )
    }

    private fun bestAudioFormat( response: JsonObject ): JsonObject? =
        response.getObject( "streamingData" )
            .getArray( "adaptiveFormats" )
            .filterIsInstance<JsonObject>()
            .filter { it.getString( "mimeType", "" ).startsWith( "audio" ) }
            .maxByOrNull { it.getLong( "bitrate", 0 ) }

    private fun adaptiveCandidate( format: JsonObject, userAgent: String ): Candidate? {
        val url = format.getString( "url" ) ?: return null
        val bytesPerSecond = format.getLong( "bitrate", 0 ) / 8
        if( bytesPerSecond <= 0 ) return null

        return Candidate(
            url,
            bytesPerSecond,
            format.getString( "contentLength" )?.toLongOrNull() ?: 0L,
            userAgent
        )
    }

    /**
     * `null` when every probed range is servable, otherwise a description of the first
     * range this stream refused.
     */
    private fun firstUnservableRange( candidate: Candidate ): String? {
        for( second in PROBE_SECONDS ) {
            val start = candidate.bytesPerSecond * second
            if( candidate.contentLength in 1..start ) continue   // track shorter than this offset

            val code = overNetwork { rangeStatus( candidate, start, start + CHUNK - 1 ) }
            if( code != 200 && code != 206 )
                return "byte range at ~${second}s returned HTTP $code (expected 200/206) - " +
                       "teaser-blocked, playback would stop near ${second}s"
        }

        // The tail must be servable too, otherwise the track cuts off before the end.
        if( candidate.contentLength > CHUNK ) {
            val code = overNetwork {
                rangeStatus( candidate, candidate.contentLength - CHUNK, candidate.contentLength - 1 )
            }
            if( code != 200 && code != 206 )
                return "tail byte range returned HTTP $code (expected 200/206) - track would cut off early"
        }

        return null
    }

    private fun rangeStatus( candidate: Candidate, start: Long, end: Long ): Int {
        val request = Request.Builder()
            .url( candidate.url )
            .header( "User-Agent", candidate.userAgent )
            .header( "Range", "bytes=$start-$end" )
            .build()
        return http.newCall( request ).execute().use { it.code }
    }
}

private const val CHUNK = 512L * 1024L     // 512KB, matches PlayerModule.CHUNK_LENGTH

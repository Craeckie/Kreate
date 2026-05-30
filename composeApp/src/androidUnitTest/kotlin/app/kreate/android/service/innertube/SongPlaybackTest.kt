package app.kreate.android.service.innertube

import app.kreate.android.service.NewPipeDownloaderImpl
import com.grack.nanojson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume.assumeNoException
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
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
import kotlin.test.assertTrue
import kotlin.test.fail


/**
 * Live-network regression test for the core "can a song actually be played?"
 * question. It exercises the real stream-resolution path used at runtime
 * ([AndroidVrStreamHelper], the `ANDROID_VR` client) against YouTube and asserts:
 *
 *  1. the video resolves (`playabilityStatus == OK`) to a direct (non-ciphered)
 *     audio stream url, and
 *  2. that url streams **past the one-minute mark** — i.e. ranged byte requests
 *     at 0s / 60s / 120s and the tail all succeed.
 *
 * Point (2) is the important one: when YouTube requires a PO token that we don't
 * attach, it serves a ~1-minute teaser then `403`s every later range, which the
 * user sees as "the song starts but stops around 1 minute". This test fails if
 * that regression returns. See the *YouTube stream resolution* section of
 * `CLAUDE.md` and `scripts/vr_probe.py`.
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

        val audio = response.getObject( "streamingData" )
            .getArray( "adaptiveFormats" )
            .filterIsInstance<JsonObject>()
            .filter { it.getString( "mimeType", "" ).startsWith( "audio" ) }
            .maxByOrNull { it.getLong( "bitrate", 0 ) }
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
     * The resolved stream serves byte ranges across the whole track — proving it
     * plays past the one-minute mark, not just the opening teaser.
     */
    @Test
    fun resolvedStreamPlaysPastOneMinute() {
        val audio = resolveAudioFormat()
        val url = audio.getString( "url" ) ?: fail( "no direct url to probe" )
        val bytesPerSecond = audio.getLong( "bitrate", 0 ) / 8
        val contentLength = audio.getString( "contentLength" )?.toLongOrNull() ?: 0L
        assertTrue( bytesPerSecond > 0, "format reported no bitrate" )

        for( second in PROBE_SECONDS ) {
            val start = bytesPerSecond * second
            if( contentLength in 1..start ) continue   // track shorter than this offset

            val code = overNetwork { rangeStatus( url, start, start + CHUNK - 1 ) }
            assertTrue(
                code == 200 || code == 206,
                "byte range at ~${second}s returned HTTP $code (expected 200/206). " +
                "YouTube is teaser-blocking this stream - playback would stop near ${second}s."
            )
        }

        // The tail must be servable too, otherwise the track cuts off before the end.
        if( contentLength > CHUNK ) {
            val code = overNetwork { rangeStatus( url, contentLength - CHUNK, contentLength - 1 ) }
            assertTrue(
                code == 200 || code == 206,
                "tail byte range returned HTTP $code (expected 200/206) - track would cut off early."
            )
        }
    }

    private fun rangeStatus( url: String, start: Long, end: Long ): Int {
        val request = Request.Builder()
            .url( url )
            .header( "User-Agent", VR_USER_AGENT )
            .header( "Range", "bytes=$start-$end" )
            .build()
        return http.newCall( request ).execute().use { it.code }
    }
}

private const val CHUNK = 512L * 1024L     // 512KB, matches PlayerModule.CHUNK_LENGTH

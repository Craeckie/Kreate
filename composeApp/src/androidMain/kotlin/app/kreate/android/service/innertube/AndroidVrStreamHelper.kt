package app.kreate.android.service.innertube

import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonWriter
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.utils.JsonUtils
import java.nio.charset.StandardCharsets


/**
 * Builds a player request for the `ANDROID_VR` InnerTube client.
 *
 * NewPipe v0.26.0 ships no `android_vr` helper, so this mirrors NewPipe's own
 * [org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper] flow using
 * only its public APIs (it mutates the [InnertubeClientRequestInfo.ofAndroidClient]
 * instance into the VR client).
 *
 * Why VR: per yt-dlp's client table, `android_vr` (clientVersion [CLIENT_VERSION])
 * requires **neither a PO token nor the JS player** (no signature cipher, no auth),
 * which makes it the most reliable pot-free path for plain audio playback — the
 * ANDROID/IOS/WEB clients now all 403 on the media GET without a GVS PO token.
 *
 * Caveats documented by yt-dlp (kept in mind by the caller's fallback chain):
 * - "Made for kids" videos are unavailable with this client.
 * - A clientVersion above 1.65 may return SABR-only streams, so it is pinned.
 */
object AndroidVrStreamHelper {

    private const val CLIENT_NAME = "ANDROID_VR"
    private const val CLIENT_VERSION = "1.65.10"
    private const val CLIENT_ID = "28"
    private const val USER_AGENT =
        "com.google.android.apps.youtube.vr.oculus/1.65.10 " +
        "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"

    /**
     * @param vrPoTokenResult optional PO token result from [com.metrolist.music.utils.potoken.PoTokenGenerator].
     *   When non-null its [visitorData][org.schabi.newpipe.extractor.services.youtube.PoTokenResult.visitorData]
     *   is used directly (skipping the extra visitor_id round-trip) and
     *   `serviceIntegrityDimensions.poToken` is added to the request body so YouTube's
     *   bot-detection challenge is satisfied.
     * @return the raw `player` response as a nanojson [JsonObject], matching the
     * shape returned by NewPipe's helpers so it slots into the existing parse path.
     */
    fun getAndroidVrPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String,
        vrPoTokenResult: org.schabi.newpipe.extractor.services.youtube.PoTokenResult? = null
    ): JsonObject {
        val info = InnertubeClientRequestInfo.ofAndroidClient().apply {
            clientInfo.clientName = CLIENT_NAME
            clientInfo.clientVersion = CLIENT_VERSION
            clientInfo.clientId = CLIENT_ID
            // android_vr does not send a clientScreen
            clientInfo.clientScreen = null
            deviceInfo.deviceMake = "Oculus"
            deviceInfo.deviceModel = "Quest 3"
            deviceInfo.osName = "Android"
            deviceInfo.osVersion = "12L"
            deviceInfo.androidSdkVersion = 32
        }

        val headers: MutableMap<String, List<String>> = mutableMapOf(
            "User-Agent" to listOf( USER_AGENT ),
            "X-Goog-Api-Format-Version" to listOf( "2" )
        )
        headers.putAll( YoutubeParsingHelper.getClientHeaders( CLIENT_ID, CLIENT_VERSION ) )

        // If a PO token is available use its visitorData directly; otherwise fetch a fresh one.
        info.clientInfo.visitorData = vrPoTokenResult?.visitorData
            ?: YoutubeParsingHelper.getVisitorDataFromInnertube(
                info,
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_URL,
                null,
                false
            )

        val builder = YoutubeParsingHelper.prepareJsonBuilder(
            localization, contentCountry, info, null
        )
        builder.value( "videoId", videoId )
               .value( "cpn", cpn )
               .value( "contentCheckOk", true )
               .value( "racyCheckOk", true )

        // Attach the player PO token when provided so YouTube's bot-detection is satisfied.
        if ( vrPoTokenResult != null ) {
            builder.`object`( "serviceIntegrityDimensions" )
                   .value( "poToken", vrPoTokenResult.playerRequestPoToken )
                   .end()
        }

        val body = JsonWriter.string( builder.done() ).toByteArray( StandardCharsets.UTF_8 )
        // android_vr uses the regular www.youtube.com host (not the gapis host).
        val url = YoutubeParsingHelper.YOUTUBEI_V1_URL + "player?" +
                  YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        val response = NewPipe.getDownloader()
            .postWithContentTypeJson( url, headers, body, localization )

        return JsonUtils.toJsonObject(
            YoutubeParsingHelper.getValidJsonResponseBody( response )
        )
    }
}

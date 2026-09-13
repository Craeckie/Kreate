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
 * Builds a player request for the `VISIONOS` InnerTube client.
 *
 * NewPipe v0.26.0 ships no `visionos` helper (it lands in v0.26.3's
 * `YoutubeStreamHelper.getVisionOsPlayerResponse` / `InnertubeClientRequestInfo.ofVisionOsClient`,
 * neither of which this app's pinned v0.26.0 has), so this mirrors that flow using only NewPipe
 * v0.26.0's public APIs — the same approach the former `AndroidVrStreamHelper` used for
 * `ANDROID_VR` (it mutates the [InnertubeClientRequestInfo.ofAndroidClient] instance into the
 * VISIONOS client).
 *
 * Why VISIONOS: yt-dlp made it the default JS-less client (`_DEFAULT_JSLESS_CLIENTS =
 * ('visionos',)`, PR #17461, 2026-08-18), replacing `android_vr` — YouTube 403s ANDROID_VR
 * 1.65.10 on every format since 2026-08-17. Like the VR client it replaces, VISIONOS
 * (clientVersion [CLIENT_VERSION]) requires **neither a PO token nor the JS player** (no
 * signature cipher, no auth), making it the most reliable pot-free path for plain audio playback.
 *
 * Caveats documented by yt-dlp (kept in mind by the caller's fallback chain):
 * - "Made for kids" videos are unavailable with this client (surfaces as UNPLAYABLE, handled by
 *   falling back to IOS).
 */
object VisionOsStreamHelper {

    private const val CLIENT_NAME = "VISIONOS"
    private const val CLIENT_VERSION = "1.02"
    private const val CLIENT_ID = "101"
    const val USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Version/26.0 Safari/605.1.15"

    /**
     * @param poTokenResult optional PO token result from [com.metrolist.music.utils.potoken.PoTokenGenerator].
     *   When non-null its [visitorData][org.schabi.newpipe.extractor.services.youtube.PoTokenResult.visitorData]
     *   is used directly (skipping the extra visitor_id round-trip) and
     *   `serviceIntegrityDimensions.poToken` is added to the request body so YouTube's
     *   bot-detection challenge is satisfied.
     * @return the raw `player` response as a nanojson [JsonObject], matching the
     * shape returned by NewPipe's helpers so it slots into the existing parse path.
     */
    fun getVisionOsPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String,
        poTokenResult: org.schabi.newpipe.extractor.services.youtube.PoTokenResult? = null
    ): JsonObject {
        val info = InnertubeClientRequestInfo.ofAndroidClient().apply {
            clientInfo.clientName = CLIENT_NAME
            clientInfo.clientVersion = CLIENT_VERSION
            clientInfo.clientId = CLIENT_ID
            // visionos does not send a clientScreen
            clientInfo.clientScreen = null
            deviceInfo.platform = null
            deviceInfo.deviceMake = "Apple"
            deviceInfo.deviceModel = "RealityDevice17,1"
            deviceInfo.osName = "visionOS"
            deviceInfo.osVersion = "26.5.23O471"
            deviceInfo.androidSdkVersion = 0
        }

        val headers: MutableMap<String, List<String>> = mutableMapOf(
            "User-Agent" to listOf( USER_AGENT ),
            "X-Goog-Api-Format-Version" to listOf( "2" )
        )
        headers.putAll( YoutubeParsingHelper.getClientHeaders( CLIENT_ID, CLIENT_VERSION ) )

        // If a PO token is available use its visitorData directly; otherwise fetch a fresh one.
        info.clientInfo.visitorData = poTokenResult?.visitorData
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
        if ( poTokenResult != null ) {
            builder.`object`( "serviceIntegrityDimensions" )
                   .value( "poToken", poTokenResult.playerRequestPoToken )
                   .end()
        }

        val body = JsonWriter.string( builder.done() ).toByteArray( StandardCharsets.UTF_8 )
        // visionos uses the regular www.youtube.com host (verified by scripts/vr_probe.py --client
        // all to stream fully, unlike the gapis host).
        val url = YoutubeParsingHelper.YOUTUBEI_V1_URL + "player?" +
                  YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        val response = NewPipe.getDownloader()
            .postWithContentTypeJson( url, headers, body, localization )

        return JsonUtils.toJsonObject(
            YoutubeParsingHelper.getValidJsonResponseBody( response )
        )
    }
}

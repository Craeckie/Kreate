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
 * NewPipe v0.26.5 ships `InnertubeClientRequestInfo.ofVisionOsClient()` /
 * `YoutubeStreamHelper.getVisionOsPlayerResponse`, but neither fits as-is: `getVisionOsPlayerResponse`
 * takes no PO token, while the caller's fallback chain (`nextFallback` in
 * `InnertubeResolvingDataSource.kt`) needs to retry this rung *with* one on `LOGIN_REQUIRED`. NewPipe's
 * `ofVisionOsClient()` also still ships the device constants from when it was added (`RealityDevice14,1`,
 * visionOS `25.6.0.23O471`), which lag current yt-dlp (`RealityDevice17,1`, `26.5.23O471` — see
 * [DEVICE_MODEL]/[OS_VERSION]). So this helper builds on `ofVisionOsClient()` as a base and overrides
 * only the fields where yt-dlp `bbc809a1` (verified 2026-09-14) differs or omits — `clientScreen` and
 * `platform` are unset here because yt-dlp's `visionos` INNERTUBE_CONTEXT sends neither, whereas
 * NewPipe's default fills both in.
 *
 * Why VISIONOS: yt-dlp made it the default JS-less client (`_DEFAULT_JSLESS_CLIENTS =
 * ('visionos',)`, PR #17461, 2026-08-18), replacing `android_vr` — YouTube 403s ANDROID_VR
 * 1.65.10 on every format since 2026-08-17. Like the VR client it replaces, VISIONOS requires
 * **neither a PO token nor the JS player** (no signature cipher, no auth), making it the most
 * reliable pot-free path for plain audio playback.
 *
 * Caveats documented by yt-dlp (kept in mind by the caller's fallback chain):
 * - "Made for kids" videos are unavailable with this client (surfaces as UNPLAYABLE, handled by
 *   falling back to IOS).
 */
object VisionOsStreamHelper {

    private const val DEVICE_MODEL = "RealityDevice17,1"
    private const val OS_VERSION = "26.5.23O471"
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
        val info = InnertubeClientRequestInfo.ofVisionOsClient().apply {
            // yt-dlp's visionos client sends neither field; NewPipe's default sets both.
            clientInfo.clientScreen = null
            deviceInfo.platform = null
            deviceInfo.deviceModel = DEVICE_MODEL
            deviceInfo.osVersion = OS_VERSION
        }

        val headers: MutableMap<String, List<String>> = mutableMapOf(
            "User-Agent" to listOf( USER_AGENT ),
            "X-Goog-Api-Format-Version" to listOf( "2" )
        )
        headers.putAll( YoutubeParsingHelper.getClientHeaders( info.clientInfo.clientId, info.clientInfo.clientVersion ) )

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

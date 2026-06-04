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
 * Builds a player request for the plain `ANDROID` InnerTube client.
 *
 * Sibling of [AndroidVrStreamHelper]; uses NewPipe's [InnertubeClientRequestInfo.ofAndroidClient]
 * defaults (clientName `ANDROID`, the bundled client version) without mutating it, and skips the
 * PO token that [org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper.getAndroidPlayerResponse]
 * insists on.
 *
 * Why this exists: for some videos `ANDROID_VR` returns `UNPLAYABLE` and the `IOS`/`WEB` clients
 * only serve a pot-blocked teaser, yet the plain `ANDROID` client still returns a **progressive**
 * muxed format (itag 18 / 22) with a *direct, un-ciphered, pot-free* URL that streams to the end.
 * That format is lower quality (≈70-100 kbps AAC, 360p video we ignore), so it is only used as a
 * last resort by the resolver's fallback chain when both VR and IOS have failed.
 */
object AndroidStreamHelper {

    fun getAndroidPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String
    ): JsonObject {
        // ofAndroidClient() already sets clientName/version/id for the ANDROID client.
        val info = InnertubeClientRequestInfo.ofAndroidClient()

        val headers: MutableMap<String, List<String>> = mutableMapOf(
            "User-Agent" to listOf( YoutubeParsingHelper.getAndroidUserAgent( localization ) ),
            "X-Goog-Api-Format-Version" to listOf( "2" )
        )
        headers.putAll(
            YoutubeParsingHelper.getClientHeaders( info.clientInfo.clientId, info.clientInfo.clientVersion )
        )

        // A valid visitorData is required for a usable player response.
        info.clientInfo.visitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
            info,
            localization,
            contentCountry,
            headers,
            YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL,
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

        val body = JsonWriter.string( builder.done() ).toByteArray( StandardCharsets.UTF_8 )
        // The ANDROID client uses the gapis host.
        val url = YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL + "player?" +
                  YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        val response = NewPipe.getDownloader()
            .postWithContentTypeJson( url, headers, body, localization )

        return JsonUtils.toJsonObject(
            YoutubeParsingHelper.getValidJsonResponseBody( response )
        )
    }
}

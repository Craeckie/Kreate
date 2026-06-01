package app.kreate.android.coil3

import app.kreate.util.isResizableThumbnail
import app.kreate.util.thumbnailResized
import coil3.Uri
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.size.pxOrElse
import kotlin.math.max

/**
 * Rewrites Google-hosted (lh3 / ggpht / yt3) thumbnail URLs to the size actually
 * resolved for the on-screen target, so a 60dp list row no longer downloads and
 * decodes a full 900px JPEG (the previous behaviour, which caused dropped frames
 * while scrolling).
 *
 * When the target size is undefined — e.g. the full-screen player requests
 * [coil3.size.Size.ORIGINAL] — it falls back to [ImageFactory.THUMBNAIL_SIZE] so
 * cover art stays sharp.
 *
 * Because [ImageFactory] sets a single size-agnostic disk-cache key, the rewritten
 * request also carries a size-aware memory/disk cache key so a 160px row and a 900px
 * cover of the same song don't overwrite each other.
 */
class ThumbnailSizeInterceptor : Interceptor {

    override suspend fun intercept( chain: Interceptor.Chain ): ImageResult {
        val url = when( val data = chain.request.data ) {
            is String -> data
            is Uri    -> data.toString()
            else      -> null
        }

        if( !url.isResizableThumbnail() )
            return chain.proceed()

        val px = max(
            chain.size.width.pxOrElse { 0 },
            chain.size.height.pxOrElse { 0 }
        ).takeIf { it > 0 } ?: ImageFactory.THUMBNAIL_SIZE

        val resized = url.thumbnailResized( px )
        if( resized == null || resized == url )
            return chain.proceed()

        val request = chain.request
                           .newBuilder()
                           .data( resized )
                           .memoryCacheKey( "$url@$px" )
                           .diskCacheKey( "$url@$px" )
                           .build()
        return chain.withRequest( request ).proceed()
    }
}

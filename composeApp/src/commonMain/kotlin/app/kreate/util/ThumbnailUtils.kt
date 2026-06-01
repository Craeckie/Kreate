package app.kreate.util

import coil3.Uri
import coil3.toUri


fun String?.thumbnail(size: Int): String? {
    return when {
        this?.startsWith("https://lh3.googleusercontent.com") == true -> "$this-w$size-h$size"
        this?.startsWith("https://yt3.ggpht.com") == true -> "$this-w$size-h$size-s$size"
        this?.startsWith("https://yt3.googleusercontent.com") == true -> "$this-w$size-h$size-s$size"
        else -> this
    }
}

/**
 * Google-hosted thumbnail URLs whose on-disk render size can be controlled by a
 * trailing size directive (`=wN-hN…`). These all serve arbitrary sizes from the same
 * base URL, so a 60dp row can request a 60px image instead of decoding a 900px one.
 */
private val RESIZABLE_THUMBNAIL_HOSTS = listOf(
    "https://lh3.googleusercontent.com",
    "https://yt3.ggpht.com",
    "https://yt3.googleusercontent.com"
)

/** Whether [this] is a Google thumbnail URL whose size we can rewrite. */
fun String?.isResizableThumbnail(): Boolean =
    this != null && RESIZABLE_THUMBNAIL_HOSTS.any( this::startsWith )

/**
 * Rewrite the size directive on a Google thumbnail URL to [size] px, **stripping any
 * pre-existing directive** so directives never stack (unlike [thumbnail], which only
 * appends). YouTube bakes a size into the URLs it returns, so stripping is required to
 * actually shrink them.
 */
fun String?.thumbnailResized(size: Int): String? {
    if( this == null ) return null

    // Everything after '=' is the size/crop directive; drop it before re-appending.
    val base = substringBefore( '=' )
    return when {
        base.startsWith( "https://lh3.googleusercontent.com" ) -> "$base=w$size-h$size"
        base.startsWith( "https://yt3.ggpht.com" ) -> "$base=w$size-h$size-s$size"
        base.startsWith( "https://yt3.googleusercontent.com" ) -> "$base=w$size-h$size-s$size"
        else -> this
    }
}
fun String?.thumbnail(): String? {
    return this
}
fun Uri?.thumbnail(size: Int): Uri? {
    return toString().thumbnail(size)?.toUri()
}
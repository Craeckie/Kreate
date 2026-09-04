@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.exoplayer.offline.Download

/**
 * Answers "does this cache actually hold the whole resource?" for either of the two media3
 * caches this app keeps — the plain playback cache a song streams into as it plays, and the
 * download-completion cache a finished download lands in (see [app.kreate.di.CacheType] for
 * both). Neither media3's
 * [androidx.media3.exoplayer.offline.DownloadIndex] nor the Room `formats` table can answer
 * this on its own.
 *
 * For the download cache: the index and the cache are independent stores. A row can say
 * [androidx.media3.exoplayer.offline.Download.STATE_COMPLETED] long after the bytes were
 * removed (see `docs/superpowers/plans/2026-09-03-download-state-desync-and-audio-only-playback.md`),
 * which is why the UI must consult both.
 *
 * For the plain playback cache: Room's `formats.contentLength` is nullable, and a progressive
 * (ANDROID itag 18/22) stream resolves with no `contentLength` in YouTube's player JSON, so the
 * column is NULL for it. A cached-bytes-equals-Room-column check can then never be true even
 * though the song plays instantly with zero network — so that check must not be used either.
 */
object DownloadCacheState {

    /**
     * `true` only when [cache] holds every byte of [key].
     *
     * The content length comes from the cache's own [ContentMetadata], not from the Room
     * `formats` table: `SimpleCache.removeResource` and a full LRU eviction drop the metadata
     * together with the spans, so a wiped resource reports `C.LENGTH_UNSET` (-1) and fails the
     * `> 0` guard. A partially evicted resource keeps its metadata but fails [Cache.isCached].
     * This also makes it the right check for a song whose Room `formats.contentLength` is NULL
     * (a progressive ANDROID-fallback stream): `CacheDataSource` writes the length into
     * [ContentMetadata] itself when it reaches EOF while writing, independent of what Room knows.
     *
     * Never throws — a released or uninitialized cache reports `false`, matching how
     * [it.fast4x.rimusic.utils.downloadedStateMedia] already treats an unreachable cache.
     */
    fun isFullyCached( cache: Cache, key: String ): Boolean =
        runCatching {
            val length = ContentMetadata.getContentLength( cache.getContentMetadata( key ) )
            length > 0 && cache.isCached( key, 0, length )
        }.getOrDefault( false )

    /**
     * The single definition of "this song is downloaded" — the one the badge renders **and**
     * the one the badge's tap acts on.
     *
     * The two must never diverge. When the index says [Download.STATE_COMPLETED] but the bytes
     * are gone, a badge that reads both stores shows "not downloaded" while an action that reads
     * only the index takes its *remove* branch: the stale row disappears, the badge does not
     * change, and the tap looks like it did nothing.
     *
     * @param indexState media3's `Download.state`, or `null` when the index has no row for [key]
     */
    fun isDownloaded( indexState: Int?, cache: Cache, key: String ): Boolean =
        indexState == Download.STATE_COMPLETED && isFullyCached( cache, key )
}

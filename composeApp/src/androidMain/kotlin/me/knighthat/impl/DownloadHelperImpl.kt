package me.knighthat.impl

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import app.kreate.android.Preferences
import app.kreate.android.coil3.ImageFactory
import app.kreate.android.service.DownloadCacheState
import app.kreate.android.service.DownloadHelper
import app.kreate.android.service.SongRematcher
import app.kreate.android.service.player.RematchRequests
import app.kreate.android.utils.isLocal
import app.kreate.database.models.Song
import app.kreate.di.CacheType
import co.touchlab.kermit.Logger
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.service.MyDownloadService
import it.fast4x.rimusic.service.UnplayableException
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.asSong
import it.fast4x.rimusic.utils.download
import it.fast4x.rimusic.utils.downloadSyncedLyrics
import it.fast4x.rimusic.utils.isNetworkConnected
import it.fast4x.rimusic.utils.removeDownload
import it.fast4x.rimusic.utils.thumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.knighthat.utils.Toaster
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.Collections
import java.util.concurrent.Executors


@OptIn(UnstableApi::class)
class DownloadHelperImpl(
    private val context: Context,
): DownloadHelper, KoinComponent {

    companion object {

        private const val TAG = "DownloadHelperImpl"
        private const val NUM_PARALLEL_DOWNLOADS = 3
        private const val NUM_RETRIES = 2
        private const val EXECUTOR_NAME = "DownloadHelper-Executor-Scope"
        private const val DB_INIT_MAX_RETRIES = 5
        private const val DB_INIT_BACKOFF_MS = 200L
    }

    private val executor = Executors.newCachedThreadPool()
    private val coroutineScope = CoroutineScope(
        executor.asCoroutineDispatcher() +
                SupervisorJob() +
                CoroutineName(EXECUTOR_NAME)
    )
    /** Tracks videoIds already attempted for download-rematch; prevents infinite retry loops. */
    private val rematchedDownloads: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val downloadCache: Cache by inject( CacheType.DOWNLOAD )

    /** Walk the cause chain looking for [UnplayableException]. */
    private fun findUnplayable( t: Throwable? ): UnplayableException? {
        if( t == null ) return null
        if( t is UnplayableException ) return t
        return findUnplayable( t.cause )
    }

    /**
     * When a download fails because the stored videoId is no longer available,
     * search for a replacement (same logic as the player's auto-rematch):
     *  - STRONG match → re-id in DB + restart download with new id.
     *  - WEAK match   → show [RematchRequests] confirmation dialog; on
     *                    acceptance re-id + restart download.
     *  - No candidates → show a toast.
     */
    private fun launchDownloadRematch( songId: String ) {
        if( !rematchedDownloads.add( songId ) ) return  // already attempted

        coroutineScope.launch {
            val deadSong: Song = Database.songTable.findById( songId ).first()
                ?: run {
                    Logger.w( "DownloadHelperImpl" ) { "Rematch: song $songId not in DB" }
                    return@launch
                }

            Logger.i( "DownloadHelperImpl" ) { "Download rematch: searching replacement for $songId (${deadSong.title})" }
            Toaster.i( context.getString( app.kreate.android.R.string.rematch_searching ) )
            val candidates = SongRematcher.searchCandidates( deadSong )
            val match      = SongRematcher.bestMatch( deadSong, candidates )

            when {
                match == null -> {
                    Logger.w( "DownloadHelperImpl" ) { "Download rematch: no candidates found for $songId" }
                    Toaster.w( context.getString( app.kreate.android.R.string.rematch_no_candidates ) )
                }

                match.confidence == SongRematcher.Confidence.STRONG -> {
                    val newId = match.item.key
                    Logger.i( "DownloadHelperImpl" ) { "Download rematch STRONG: $songId → $newId" }
                    Database.reIdSong( songId, newId, match.item )
                    // Remove the failed download entry, then re-download with new id
                    removeDownload( deadSong.asMediaItem )
                    addDownload( match.item.asMediaItem )
                    Toaster.i( context.getString( app.kreate.android.R.string.rematch_replaced_unavailable_track ) )
                }

                else -> {
                    Logger.i( "DownloadHelperImpl" ) { "Download rematch WEAK: asking user for $songId" }
                    RematchRequests.emit(
                        RematchRequests.Request(
                            deadSong   = deadSong,
                            candidates = candidates,
                            onAccepted = { chosen ->
                                val newId = chosen.key
                                coroutineScope.launch {
                                    Database.reIdSong( songId, newId, chosen )
                                    removeDownload( deadSong.asMediaItem )
                                    addDownload( chosen.asMediaItem )
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    /** media3 reports download state as an int; logs are useless without the name. */
    private fun stateName( state: Int ): String = when( state ) {
        Download.STATE_QUEUED       -> "QUEUED"
        Download.STATE_STOPPED      -> "STOPPED"
        Download.STATE_DOWNLOADING  -> "DOWNLOADING"
        Download.STATE_COMPLETED    -> "COMPLETED"
        Download.STATE_FAILED       -> "FAILED"
        Download.STATE_REMOVING     -> "REMOVING"
        Download.STATE_RESTARTING   -> "RESTARTING"
        else                        -> "UNKNOWN($state)"
    }

    override val downloads: MutableStateFlow<Map<String, Download>>
    override val downloadManager by lazy {
        val listener = object: DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                // Info level: release builds log at Info, and a download that silently never
                // starts is otherwise invisible in a user-supplied logcat.
                Logger.i( TAG ) {
                    "download ${download.request.id} -> ${stateName( download.state )}" +
                    " (${download.percentDownloaded.toInt()}%, ${download.bytesDownloaded}B)" +
                    " queued=${downloadManager.currentDownloads.size}/${downloadManager.maxParallelDownloads}" +
                    ( finalException?.let { " exception=${it::class.simpleName}: ${it.message}" } ?: "" )
                }

                syncDownloads( download )
                // Clear rematch guard on success so a future failure of the same id
                // can re-trigger (otherwise the guard set grows unboundedly).
                if( download.state == Download.STATE_COMPLETED ) {
                    rematchedDownloads.remove( download.request.id )
                }
                if( download.state == Download.STATE_FAILED
                    && findUnplayable( finalException ) != null
                ) {
                    launchDownloadRematch( download.request.id )
                }
            }

            override fun onDownloadRemoved(
                downloadManager: DownloadManager,
                download: Download
            ) {
                Logger.i( TAG ) { "download ${download.request.id} removed from index" }
                syncDownloads( download )
            }
        }

        val manager = DownloadManager(
            context,
            StandaloneDatabaseProvider(context),
            get(CacheType.DOWNLOAD),
            get<ResolvingDataSource.Factory>(),
            executor
        )

        manager.maxParallelDownloads = NUM_PARALLEL_DOWNLOADS
        manager.minRetryCount = NUM_RETRIES
        manager.requirements = Requirements(Requirements.NETWORK)
        manager.addListener( listener )

        Logger.i( TAG ) {
            "DownloadManager ready: maxParallel=${manager.maxParallelDownloads}" +
            " retries=${manager.minRetryCount} requirementsMet=${manager.isWaitingForRequirements.not()}"
        }

        manager
    }

    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    init {
        val results = mutableMapOf<String, Download>()

        // Retry with backoff to handle transient SQLITE_BUSY during
        // startup when other components are still initializing the DB.
        var lastException: Exception? = null
        for (attempt in 1..DB_INIT_MAX_RETRIES) {
            try {
                val cursor = downloadManager.downloadIndex.getDownloads()
                try {
                    while (cursor.moveToNext()) {
                        results[cursor.download.request.id] = cursor.download
                    }
                } finally {
                    cursor.close()
                }
                lastException = null
                break
            } catch (e: android.database.sqlite.SQLiteDatabaseLockedException) {
                lastException = e
                Logger.w("DownloadHelperImpl") { "DB locked during init (attempt $attempt/$DB_INIT_MAX_RETRIES), retrying..." }
                Thread.sleep(DB_INIT_BACKOFF_MS * attempt)
            }
        }
        if (lastException != null) {
            Logger.e(lastException, "DownloadHelperImpl") { "Failed to read download index after $DB_INIT_MAX_RETRIES attempts" }
        }
        downloads = MutableStateFlow(results) // Proceed with whatever we got (possibly empty)

        // The index and the download cache are independent stores. Log how far apart they start
        // out: *every* COMPLETED row reporting no bytes means the cache's own index failed to
        // load, while a handful means real byte loss (an old LRU eviction, a manual wipe). Info
        // level, because this is the discriminator a "download button does nothing" report needs
        // and release builds drop anything below it.
        runCatching {
            val completed = results.values.count { it.state == Download.STATE_COMPLETED }
            val stale = results.values.count {
                it.state == Download.STATE_COMPLETED
                        && !DownloadCacheState.isFullyCached( downloadCache, it.request.id )
            }
            Logger.i( TAG ) {
                "download index: ${results.size} rows, $completed completed, $stale of those" +
                " hold no bytes in the download cache" +
                " (cacheSpace=${downloadCache.cacheSpace}B, keys=${downloadCache.keys.size})"
            }
        }.onFailure {
            Logger.w( TAG ) { "Could not reconcile download index against cache: ${it.message}" }
        }
    }

    @Synchronized
    private fun syncDownloads( download: Download ) =
        downloads.update { map ->
            map.toMutableMap().apply {
                set(download.request.id, download)
            }
        }

    override fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    override fun getDownloadNotificationHelper(): DownloadNotificationHelper {
        if (!::downloadNotificationHelper.isInitialized) {
            downloadNotificationHelper =
                DownloadNotificationHelper(context, DownloadHelper.DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        }
        return downloadNotificationHelper
    }

    override fun addDownload( mediaItem: MediaItem ) {
        if (mediaItem.isLocal) {
            Logger.i( TAG ) { "addDownload ${mediaItem.mediaId} skipped: local file" }
            return
        }

        if( !isNetworkConnected( context ) ) {
            Logger.i( TAG ) { "addDownload ${mediaItem.mediaId} refused: no network" }
            Toaster.noInternet()
            return
        }

        Logger.i( TAG ) {
            "addDownload ${mediaItem.mediaId} requested" +
            " (already known: ${downloads.value[mediaItem.mediaId]?.let { stateName( it.state ) } ?: "no"})"
        }

        val downloadRequest = DownloadRequest
            .Builder(
                /* id      = */ mediaItem.mediaId,
                /* uri     = */ mediaItem.mediaId.toUri()
            )
            .setCustomCacheKey(mediaItem.mediaId)
            .setData("${mediaItem.mediaMetadata.artist.toString()} - ${mediaItem.mediaMetadata.title.toString()}".encodeToByteArray()) // Title in notification
            .build()

        Database.asyncTransaction {
            insertIgnore( mediaItem )
        }

        val imageUrl = mediaItem.mediaMetadata.artworkUri.thumbnail(1200)

//            sendAddDownload(
//                context,MyDownloadService::class.java,downloadRequest,false
//            )

        coroutineScope.launch {
            val sendResult = context.download<MyDownloadService>(downloadRequest)
            sendResult.exceptionOrNull()
                ?.let {
                    if (it is CancellationException) throw it

                    Logger.e( it, TAG ) { "addDownload ${mediaItem.mediaId}: could not reach MyDownloadService" }
                }
                ?: Logger.i( TAG ) { "addDownload ${mediaItem.mediaId}: handed to MyDownloadService" }
            downloadSyncedLyrics( mediaItem.asSong )

            ImageFactory.requestBuilder( imageUrl.toString() ) {
                bitmapConfig( Bitmap.Config.ARGB_8888 )
                allowHardware( false )
            }
        }
    }

    override fun removeDownload( mediaItem: MediaItem ) {
        if (mediaItem.isLocal) return

        //sendRemoveDownload(context,MyDownloadService::class.java,mediaItem.mediaId,false)
        coroutineScope.launch {
            context.removeDownload<MyDownloadService>(mediaItem.mediaId).exceptionOrNull()?.let {
                if (it is CancellationException) throw it

                Logger.e( it, "DownloadHelperImpl" ) { "removeDownload failed!"}
            }
        }
    }

    override fun purgeDownload( songId: String ) {
        coroutineScope.launch {
            if ( songId in downloads.value ) {
                // DownloadManager owns the bytes for anything it has an index row for;
                // it removes the row and the cached resource together.
                context.removeDownload<MyDownloadService>( songId ).exceptionOrNull()?.let {
                    if (it is CancellationException) throw it

                    Logger.e( it, "DownloadHelperImpl" ) { "purgeDownload failed for $songId!" }
                }
            } else {
                // No index row, so DownloadManager.removeDownload() would be a no-op and the
                // bytes would linger forever. Free them directly — nothing else can.
                runCatching { downloadCache.removeResource( songId ) }
                    .onFailure {
                        Logger.w( "DownloadHelperImpl" ) { "Could not evict orphaned bytes for $songId: ${it.message}" }
                    }
            }
        }
    }

    override fun purgeAllDownloads() {
        coroutineScope.launch {
            runCatching {
                DownloadService.sendRemoveAllDownloads(
                    /* context    = */ context,
                    /* clazz      = */ MyDownloadService::class.java,
                    /* foreground = */ false
                )
            }.onFailure {
                if (it is CancellationException) throw it

                Logger.e( it, "DownloadHelperImpl" ) { "purgeAllDownloads failed!" }
            }
        }
    }

    override fun autoDownload( mediaItem: MediaItem ) {
        if ( Preferences.AUTO_DOWNLOAD.value ) {
            // Same both-stores predicate as the badge: a COMPLETED row whose bytes are gone
            // must download again, not be skipped as already present.
            val indexState = downloads.value[mediaItem.mediaId]?.state
            if( !DownloadCacheState.isDownloaded( indexState, downloadCache, mediaItem.mediaId ) )
                addDownload( mediaItem )
        }
    }

    override fun autoDownloadWhenLiked( mediaItem: MediaItem ) {
        if ( Preferences.AUTO_DOWNLOAD_ON_LIKE.value ) {
            Database.asyncQuery {
                runBlocking {
                    if( songTable.isLiked( mediaItem.mediaId ).first() )
                        autoDownload(mediaItem)
                    else
                        removeDownload(mediaItem)
                }
            }
        }
    }

    override fun downloadOnLike( mediaItem: MediaItem, likeState: Boolean? ) {
        // Only continues when this setting is enabled
        val isSettingEnabled by Preferences.AUTO_DOWNLOAD_ON_LIKE
        if( !isSettingEnabled || !isNetworkConnected( context ) )
            return

        // [likeState] is a tri-state value,
        // only `true` represents like, so
        // `true` must be value set to download
        if( likeState == true )
            autoDownload( mediaItem)
        else
            removeDownload( mediaItem)
    }

    override fun handleDownload( song: Song, removeIfDownloaded: Boolean ) {
        if( song.isLocal ) {
            Logger.i( TAG ) { "handleDownload ${song.id} ignored: local file" }
            return
        }

        val indexState = downloads.value[song.id]?.state
        // Must be the predicate the badge renders. Deciding from the index alone makes a tap on
        // a badge that reads "not downloaded" remove a stale COMPLETED row instead of starting
        // the download — the badge never changes and the button appears dead. media3 re-queues
        // an existing COMPLETED row on addDownload, so the stale case needs no removal first.
        val isDownloaded = DownloadCacheState.isDownloaded( indexState, downloadCache, song.id )

        Logger.i( TAG ) {
            "handleDownload ${song.id}: index=${indexState?.let( ::stateName ) ?: "none"}" +
            " downloaded=$isDownloaded removeIfDownloaded=$removeIfDownloaded"
        }

        if( isDownloaded && removeIfDownloaded )
            removeDownload( song.asMediaItem )
        else if( !isDownloaded )
            addDownload( song.asMediaItem )
    }
}
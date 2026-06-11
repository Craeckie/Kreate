package app.kreate.android.service.player

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.media3.common.AuxEffectInfo
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import app.kreate.di.CacheType
import app.kreate.di.clearCachedStreamUrlOf
import app.kreate.android.Preferences
import app.kreate.android.R
import app.kreate.android.service.PlayerEventUpdateDiscord
import app.kreate.android.service.SongRematcher
import app.kreate.android.service.playback.PlaybackListener
import app.kreate.android.service.playback.PlaybackService
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import app.kreate.android.utils.innertube.toMediaItem
import app.kreate.android.utils.isLocal
import app.kreate.android.widget.WidgetReceiver
import app.kreate.database.models.PersistentQueue
import app.kreate.database.models.Song
import app.kreate.di.PrefType
import co.touchlab.kermit.Logger
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.QueueLoopType
import it.fast4x.rimusic.service.LoginRequiredException
import it.fast4x.rimusic.service.MissingDecipherKeyException
import it.fast4x.rimusic.service.NoInternetException
import it.fast4x.rimusic.service.PlayableFormatNotFoundException
import it.fast4x.rimusic.service.UnknownException
import it.fast4x.rimusic.service.UnplayableException
import it.fast4x.rimusic.utils.AppLifecycleTracker
import it.fast4x.rimusic.utils.TimerJob
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.getEnum
import it.fast4x.rimusic.utils.mediaItems
import it.fast4x.rimusic.utils.playNext
import it.fast4x.rimusic.utils.setGlobalVolume
import it.fast4x.rimusic.utils.timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.knighthat.innertube.Innertube
import me.knighthat.innertube.model.InnertubeSong
import me.knighthat.utils.Toaster
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * A custom ExoPlayer with additional features:
 * - Fading effect
 * - Observable states (current mediaItem, timeline, window, etc.)
 */
@OptIn(UnstableApi::class)
class StatefulPlayerImpl(
    private val context: Context,
    private val player: ExoPlayer
) : ExoPlayer by player,
    StatefulPlayer,
    Player.Listener,
    KoinComponent,
    SharedPreferences.OnSharedPreferenceChangeListener
{

    companion object {
        // TODO: Make this a setting entry
        private const val SAVE_INTERVAL = 30_000L

        private const val SLEEP_TIMER_NOTIFICATION_ID = 1002
        private const val SLEEP_TIMER_NOTIFICATION_CHANNEL = "sleep_timer_channel_id"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logger = Logger.withTag("StatefulPlayer")
    private val _currentMediaItemState = MutableStateFlow<MediaItem?>(null)
    private val _currentTimelineState = MutableStateFlow(Timeline.EMPTY)
    private val _currentWindowState = MutableStateFlow<Timeline.Window?>(null)
    private val _isPlayingState = MutableStateFlow(false)
    private val _isPersistentQueueEnabled = MutableStateFlow(false)

    //<editor-fold desc="Jobs">
    private var volumeAnimator: ValueAnimator? = null
    private var radioJob: Job? = null
    private var timerJob: TimerJob? = null
    private var loudnessNormalizationJob: Job? = null
    private var bassBoostJob: Job? = null
    private var reverbJob: Job? = null
    //</editor-fold>
    //<editor-fold desc="AudioFX">
    private lateinit var loudnessEnhancer: LoudnessEnhancer
    private lateinit var bassBoost: BassBoost
    private lateinit var reverb: PresetReverb
    //</editor-fold>
    private var errorTimestamp = 0L
    private var lastErrorMessage = ""
    private val retried403Songs  = mutableSetOf<String>()
    /** Songs for which we already attempted an automatic rematch this session. */
    private val rematchedSongs   = mutableSetOf<String>()
    /** Songs whose corrupted streaming-cache entry we already evicted this session. */
    private val cacheRecoveredSongs = mutableSetOf<String>()
    /** Writable streaming cache (not the read-only download cache); used to evict corrupt entries. */
    private val streamCache: Cache by inject( CacheType.CACHE )

    override val currentMediaItemState = _currentMediaItemState.asStateFlow()
    override val currentTimelineState = _currentTimelineState.asStateFlow()
    override val currentWindowState = _currentWindowState.asStateFlow()
    override val isPlayingState = _isPlayingState.asStateFlow()

    init {
        this.addListener( this )
        this.addListener( PlayerEventUpdateDiscord() )
        this.addAnalyticsListener(
            PlaybackStatsListener(false, PlaybackListener(coroutineScope))
        )

        val preferences: SharedPreferences by inject(PrefType.DEFAULT)
        preferences.registerOnSharedPreferenceChangeListener( this )
        _isPersistentQueueEnabled.value = preferences.getBoolean(Preferences.Key.ENABLE_PERSISTENT_QUEUE, false)

        skipSilenceEnabled = Preferences.AUDIO_SKIP_SILENCE.value
        repeatMode = Preferences.QUEUE_LOOP_TYPE.value.type
        shuffleModeEnabled = Preferences.PLAYER_SHUFFLE.value
        volume = Preferences.AUDIO_VOLUME.value
        setGlobalVolume( volume )
        playbackParameters = PlaybackParameters(
            Preferences.AUDIO_SPEED_VALUE.value,
            Preferences.AUDIO_PITCH.value
        )

        loadPersistentQueue()
        schedulePersistentQueueSave()

        if( Preferences.ENABLE_PERSISTENT_QUEUE.value
            && Preferences.RESUME_PLAYBACK_ON_STARTUP.value
            && AppLifecycleTracker.isInForeground()
        ) play()
    }

    private fun stopFadingEffect() {
        volumeAnimator?.cancel()
        volumeAnimator = null
    }

    /**
     * Calculates the perceptually scaled volume based on a linear progress.
     * This uses a logarithmic curve to make the fade sound more natural.
     *
     * @param linearProgress A linear value from 0.0f to 1.0f representing the fade progress.
     * @param startVolume The starting volume for the current fade.
     * @param targetVolume The target volume for the current fade.
     * @return The volume value (0.0f to 1.0f) to set on ExoPlayer.
     */
    private fun getVolumeForProgress(
        linearProgress: Float,
        startVolume: Float,
        targetVolume: Float
    ): Float {
        // Adjust this factor to change the steepness of the curve.
        // A common range is 2.0 to 4.0. Higher values make the lower end steeper.
        val curveFactor = 3.0f

        // Apply a power curve (logarithmic perception)
        // This formula maps a linear input (linearProgress) to a more perceptually linear output.
        val scaledProgress = if (linearProgress <= 0f) {
            0f
        } else if (linearProgress >= 1f) {
            1f
        } else {
            (10f.pow(curveFactor * linearProgress) - 1f) / (10f.pow(curveFactor) - 1f)
        }

        // Interpolate between start and target volume using the scaled progress
        return startVolume + (targetVolume - startVolume) * scaledProgress
    }

    /**
     * Starts a volume fade from a start volume to a target volume over a specified duration.
     * The fade uses a logarithmic curve for perceptual smoothness.
     *
     * @param start The volume to start the fade from (0.0f to 1.0f).
     * @param end The volume to fade to (0.0f to 1.0f).
     */
    @MainThread
    private fun startFade(
        start: Float,
        end: Float,
        durationInMillis: Long,
        doOnStart: (Animator) -> Unit = {},
        doOnEnd: (Animator) -> Unit = {}
    ) {
        stopFadingEffect()

        if( durationInMillis == 0L )
            return

        with( ValueAnimator.ofFloat( 0f, 1f ) ) {
            volumeAnimator = this

            duration = durationInMillis
            addUpdateListener { animator ->
                val floor = min(start, end)
                val ceil = max(start, end)

                volume = getVolumeForProgress(
                    linearProgress = animator.animatedValue as Float,
                    startVolume = start,
                    targetVolume = end
                ).coerceIn( floor, ceil )
            }
            doOnStart( doOnStart )
            doOnEnd( doOnEnd )

            start()
        }
    }

    private fun loadPersistentQueue() {
        if ( Preferences.ENABLE_PERSISTENT_QUEUE.value )
            logger.d { "Persistent queue enabled! Loading from database..." }
        else
            return

        coroutineScope.launch {
            val queue = Database.queueTable.blockingItems()

            if( queue.isEmpty() ) {
                logger.i { "Persistent queue empty, not resuming!" }
                return@launch
            }

            val startIndex = queue.indexOfFirst { it.position != null }.coerceAtLeast( 0 )
            val startPositionMs = queue[startIndex].position ?: C.TIME_UNSET
            val mediaItems = withContext( Dispatchers.Default ) {
                queue.map { queueItem ->
                    queueItem.song
                             .asMediaItem
                             .buildUpon()
                             .setTag( PersistentQueue.Tag )
                             .build()
                }
            }
            // Involves Player's call, must happen on main thread
            withContext( Dispatchers.Main ) {
                setMediaItems( mediaItems, startIndex, startPositionMs )
                prepare()
            }
        }
    }

    @AnyThread
    private fun savePersistentQueue(queue: List<MediaItem>, index: Int, position: Long?) {
        if( queue.isEmpty() || !Preferences.ENABLE_PERSISTENT_QUEUE.value )
            return

        val items = queue.fastMapIndexed { i, item ->
            PersistentQueue(item.mediaId, if( index == i ) position else null)
        }
        Database.asyncTransaction {
            queueTable.deleteAll()
            queue.forEach( ::insertIgnore )
            queueTable.insertIgnore( items )

            logger.d { "Queue saved!" }
        }
    }

    @MainThread
    private fun savePersistentQueue() {
        val queue = mediaItems.toList()
        val index = currentMediaItemIndex
        val position = currentPosition

        coroutineScope.launch( Dispatchers.Default ) {
            savePersistentQueue( queue, index, position )
        }
    }

    private fun schedulePersistentQueueSave() {
        coroutineScope.launch(Dispatchers.IO) {
            combine(_isPlayingState, _isPersistentQueueEnabled) { a, b -> a && b }
            .collectLatest {
                if( !it ) {
                    logger.v { "Schedule for saving persistent queue has stopped!" }
                    return@collectLatest
                }

                while( true ) {
                    val (queue, index, position) = withContext(Dispatchers.Main) {
                        Triple(mediaItems.toList(), currentMediaItemIndex, currentPosition)
                    }
                    savePersistentQueue( queue, index, position )

                    delay( SAVE_INTERVAL )
                }
            }
        }
    }

    /*
            StatefulPlayer
     */

    override fun isLoadingRadio(): Boolean = radioJob?.isActive == true

    override fun startRadio() { currentMediaItem?.let( ::startRadio ) }

    override fun startRadio(
        mediaItem: MediaItem,
        append: Boolean,
        endpoint: NavigationEndpoint.Endpoint.Watch?
    ) {
        this.stopRadio()

        // Play song immediately while other songs are being loaded
        if( currentMediaItem?.mediaId != mediaItem.mediaId )
            forcePlay( mediaItem )

        // Prevent UI from freezing up while data is being fetched
        radioJob = coroutineScope.launch {
            Innertube.radio(
                mediaItem.mediaId,
                CURRENT_LOCALE,
                endpoint?.playlistId ?: "RDAMVM${mediaItem.mediaId}",
                endpoint?.params
            ).onSuccess { relatedSongs ->
                // Launch another coroutine to make it run
                // in parallel with the rest of of block.
                launch( Dispatchers.IO ) {
                    relatedSongs.fastForEach {
                        Database.upsert( it )
                    }
                }

                // Any call to [player] must happen on Main thread
                val currentQueue = withContext( Dispatchers.Main ) {
                    mediaItems.fastMap( MediaItem::mediaId )
                }

                // Songs with the same id as provided [Song] should be removed.
                // The song usually lives at the the first index, but this
                // way is safer to implement, as it can live through changes in position.
                relatedSongs.dropWhile { it.id == mediaItem.mediaId || it.id in currentQueue }
                            .fastMap( InnertubeSong::toMediaItem )
                            .also {
                                // Any call to [player] must happen on Main thread
                                withContext( Dispatchers.Main ) {
                                    /*
                                        There are 2 possible outcomes when append is not enabled.
                                        User starts radio on currently playing song,
                                        or on a completely different song.

                                        When radio is activated on the same song, remain position
                                        of currently playing song, delete next songs, and append
                                        it with new songs.

                                        When new song is used for radio, replace entire queue with new songs.
                                      */
                                    val curIndex = currentMediaItemIndex
                                    val endIndex = mediaItemCount
                                    if( !append && mediaItemCount > 1 ) {
                                        moveMediaItem( curIndex, 0 )
                                        removeMediaItems( curIndex + 1, endIndex )
                                    }

                                    addMediaItems(it)
                                }
                            }
            }.onFailure { err ->
                logger.e( "", err )
                Toaster.e( R.string.error_song_radio_failed )
            }
        }
    }

    override fun startRadio(
        song: Song,
        append: Boolean,
        endpoint: NavigationEndpoint.Endpoint.Watch?
    ) = startRadio( song.asMediaItem, append, endpoint )

    override fun stopRadio() {
        radioJob?.cancel()
        radioJob = null
    }

    override fun cycleRepeatMode() {
        repeatMode = when( repeatMode ) {
            REPEAT_MODE_OFF -> REPEAT_MODE_ONE
            REPEAT_MODE_ONE -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_OFF
            // "else" shouldn't be executed at all,
            // if app crashes here, something went wrong, really wrong.
            else -> throw IllegalStateException()
        }

        if( repeatMode != REPEAT_MODE_OFF )
            shuffleModeEnabled = false
    }

    override fun toggleShuffleMode() {
        shuffleModeEnabled = !shuffleModeEnabled

        if( shuffleModeEnabled )
            repeatMode = REPEAT_MODE_OFF
    }

    override fun startSleepTimer( duration: Duration ) {
        val context: Context by inject()
        val title = context.getString( R.string.sleep_timer_ended )
        timerJob = coroutineScope.timer( duration.inWholeMilliseconds ) {
            pause()

            val notification = NotificationCompat
                .Builder(context, SLEEP_TIMER_NOTIFICATION_CHANNEL)
                .setContentTitle(title)
                .setAutoCancel( true )
                .setOnlyAlertOnce( true )
                .setShowWhen( true )
                .setSmallIcon( R.drawable.time )
                .build()
            val manager = context.getSystemService<NotificationManager>()
            manager?.notify( SLEEP_TIMER_NOTIFICATION_ID, notification )
        }
    }

    override fun stopSleepTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun sleepTimerRemaining(): Flow<Long?> = timerJob?.millisLeft ?: flowOf( null )

    override fun toForwardingPlayer(): ForwardingPlayer = ForwardingPlayerImpl()

    /*
            ExoPlayer
     */

    override fun play() {
        fun action() {
            if( playbackState == Player.STATE_IDLE )
                prepare()
            player.play()
        }

        val duration = Preferences.AUDIO_FADE_DURATION.value.asMillis
        if( duration == 0L ) {
            action()
            return
        }

        startFade(
            start = 0f,
            end = volume,
            durationInMillis = duration,
            doOnStart = {
                volume = 0f
                action()
            }
        )
    }

    override fun pause() {
        val duration = Preferences.AUDIO_FADE_DURATION.value.asMillis
        if( duration == 0L ) {
            player.pause()
            return
        }

        val originalVolume = volume
        startFade(
            start = volume,
            end = 0f,
            durationInMillis = duration,
            doOnEnd = {
                player.pause()
                volume = originalVolume
            }
        )
    }

    override fun getBufferedPercentage(): Int =
        try {
            player.bufferedPercentage
        } catch ( e: IllegalArgumentException ) {
            Logger.e( "", e, this::class.java.simpleName )
            0
        }

    override fun getAudioSessionId(): Int = player.audioSessionId

    override fun stop() {
        pause()

        stopRadio()
        stopSleepTimer()

        savePersistentQueue()

        loudnessNormalizationJob?.cancel()
        loudnessNormalizationJob = null
        bassBoostJob?.cancel()
        bassBoostJob = null
        reverbJob?.cancel()
        reverbJob = null

        player.stop()
    }

    override fun release() {
        stopFadingEffect()

        coroutineScope.cancel()

        player.removeListener( this )

        loudnessEnhancer.release()      // Must release after listener is removed to prevent race condition
        bassBoost.release()
        reverb.release()
        clearAuxEffectInfo()

        player.release()

        val preferences: SharedPreferences by inject(PrefType.DEFAULT)
        preferences.registerOnSharedPreferenceChangeListener( this )
    }

    /*
            Player listener
     */

    private fun normalizeLoudness() {
        if( !::loudnessEnhancer.isInitialized || !loudnessEnhancer.enabled )
            return
        else
            logger.v { "Normalizing loudness..." }

        try {
            loudnessNormalizationJob?.cancel()

            // Interaction with [Player] must happen on Main thread
            val mediaId = currentMediaItem?.mediaId ?: return
            loudnessNormalizationJob = coroutineScope.launch {
                // This holds the job as long as loudnessDb is unavailable
                val mediaLoudness: Float = Database.formatTable
                                                   .findBySongId( mediaId )
                                                   .mapNotNull { it?.loudnessDb }
                                                   .first()
                val targetLoudness by Preferences.AUDIO_VOLUME_NORMALIZATION_TARGET
                val targetGain = (targetLoudness - mediaLoudness) * 100f
                loudnessEnhancer.setTargetGain( targetGain.toInt() )

                logger.d { "Media loudness: %.2f, target loudness: %.2f, gain: %.2f".format(mediaLoudness, targetLoudness, targetGain) }
            }
        } catch( err: Exception ) {
            logger.e( err ) { "normalizeLoudness failed!" }
            Toaster.e( R.string.error_loudness_normalization_failed )
        }
    }

    private fun boostLowFrequencies() {
        if( !::bassBoost.isInitialized || !bassBoost.enabled )
            return
        else
            logger.v { "Boosting low frequency..." }

        try {
            bassBoostJob?.cancel()

            bassBoostJob = coroutineScope.launch {
                val setting by Preferences.AUDIO_BASS_BOOST_LEVEL
                val target = (setting * 1000f).coerceIn( 0f, 1000f ).toInt().toShort()
                bassBoost.setStrength( target )

                logger.d { "Bass boost strength: $target" }
            }
        } catch( err: Exception ) {
            logger.e( err ) { "boostLowFrequencies failed!" }
            Toaster.e( R.string.error_bass_boost_failed )
        }
    }

    private fun updateReverb() {
        if( !::reverb.isInitialized || !reverb.enabled )
            return
        else
            logger.v { "Updating reverb..." }

        try {
            reverbJob?.cancel()

            reverbJob = coroutineScope.launch {
                val preset by Preferences.AUDIO_REVERB_PRESET
                reverb.preset = preset.toShort()

                logger.d { "Reverb set to $preset" }
            }
        } catch( err: Exception ) {
            logger.e( err ) { "updateReverb failed!" }
            Toaster.e( R.string.error_reverb_failed )
        }
    }

    private fun updateMediaControl() {
        val intent = Intent(context, PlaybackService::class.java)
            .setAction( PlaybackService.ACTION_UPDATE_MEDIA_CONTROL )
        context.startService( intent )
    }

    private fun traverseErrorStack( t: Throwable ): Throwable =
        when( t ) {
            is PlayableFormatNotFoundException,
            is UnplayableException,
            is LoginRequiredException,
            is NoInternetException,
            is UnknownException,
            is MissingDecipherKeyException -> t

            else -> t.cause?.let( ::traverseErrorStack ) ?: t
        }

    /**
     * `true` when any throwable in [error]'s cause chain crashed inside
     * `SimpleCache.commitFile` — media3's `IllegalStateException` (failed `isFullyLocked`
     * assertion) raised when the song's videoId-keyed cache entry holds spans from a
     * different physical stream than the one currently being written. It surfaces as a
     * generic ExoPlayer "Source error", so it must be matched on the stack, not the type.
     */
    private fun isCacheCommitError( error: Throwable ): Boolean {
        var t: Throwable? = error
        while( t != null ) {
            if( t.stackTrace.any {
                    it.className == "androidx.media3.datasource.cache.SimpleCache"
                    && it.methodName == "commitFile"
                } )
                return true
            t = t.cause
        }
        return false
    }

    private fun printErrorMessage( errMsg: String )  {
        // If the same error is set within 10s, it'll be ignored.
        val timeWindow = errorTimestamp + 10.seconds.inWholeMilliseconds

        if( errMsg == lastErrorMessage
            && System.currentTimeMillis() <= timeWindow
        ) return

        lastErrorMessage = errMsg
        // When field is successfully set, update timestamp.
        errorTimestamp = System.currentTimeMillis()
        // Finally, print the error if not blank
        if( errMsg.isNotBlank() )
            Toaster.e( errMsg, Toast.LENGTH_LONG )
    }

    override fun onMediaItemTransition( mediaItem: MediaItem?, reason: Int ) {
        normalizeLoudness()
        savePersistentQueue()

        // Don't update [_currentMediaItemState] when on repeat
        if( reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT ) {
            _currentMediaItemState.update { mediaItem }
            _currentWindowState.update {
                mediaItem?.let {
                    _currentTimelineState.value.getWindow( currentMediaItemIndex, Timeline.Window() )
                }
            }
        }

        /*
            Don't fetch more item if:
            - Feature is disabled
            - When song is repeated
            - Start new queue
            - Is a local song
         */
        if( Preferences.PLAYER_ACTION_START_RADIO.value
            && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            && reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
            && currentMediaItem?.isLocal == false
        ) {
            val positionToLast = mediaItemCount - currentMediaItemIndex
            // Make sure only add when about 10 songs to the last song in queue
            // TODO: Add slider in settings to let user change number of songs
            if( positionToLast <= 10 && !isLoadingRadio() )
                startRadio()
        }

        updateMediaControl()
    }

    override fun onIsPlayingChanged( isPlaying: Boolean ) {
        _isPlayingState.update { isPlaying }

        //<editor-fold desc="Save position to persistent queue (on pause & is enabled)">
        if( !isPlaying && Preferences.ENABLE_PERSISTENT_QUEUE.value ) {
            val songId = currentMediaItem?.mediaId
            val position = currentPosition
            Database.asyncTransaction {
                songId ?: return@asyncTransaction
                queueTable.updatePosition(songId, position)

                logger.d { "Updated $songId's position to $position in persistent queue" }
            }
        }
        //</editor-fold>
        //<editor-fold desc="Update widgets">
        val metadataBundle = currentMediaItem?.mediaMetadata?.toBundle()
         val intent = Intent(WidgetReceiver.ACTION_UPDATE).apply {
            putExtra(WidgetReceiver.KEY_IS_PLAYING, isPlaying)
            putExtra(WidgetReceiver.KEY_METADATA, metadataBundle)
            `package` = context.packageName
        }
        context.sendBroadcast( intent )
        //</editor-fold>
    }

    override fun onTimelineChanged( timeline: Timeline, reason: Int ) =
        _currentTimelineState.update { timeline }

    override fun onAudioSessionIdChanged( audioSessionId: Int ) {
        logger.v { "Audio session id changed!" }

        //<editor-fold desc="Loudness enhancer">
        try {
            if( ::loudnessEnhancer.isInitialized )
                loudnessEnhancer.release()

            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            loudnessEnhancer.enabled = Preferences.AUDIO_VOLUME_NORMALIZATION.value

            normalizeLoudness()
        } catch( err: Exception ) {
            logger.e( err ) { "LoudnessEnhancer init failed!" }
        }
        //</editor-fold>
        //<editor-fold desc="Bass boost">
        try {
            if( ::bassBoost.isInitialized )
                bassBoost.release()

            bassBoost = BassBoost(0, audioSessionId)
            bassBoost.enabled = Preferences.AUDIO_BASS_BOOSTED.value

            boostLowFrequencies()
        } catch( err: Exception ) {
            logger.e( err ) { "BassBoost init failed!" }
        }
        //</editor-fold>
        //<editor-fold desc="Reverb preset">
        try {
            if( ::reverb.isInitialized )
                reverb.release()

            reverb = PresetReverb(1, audioSessionId)
            reverb.enabled = true       // Value is set by presets

            val auxEffect = AuxEffectInfo(reverb.id, 1f)
            setAuxEffectInfo( auxEffect )

            updateReverb()
        } catch( err: Exception ) {
            logger.e( err ) { "Reverb init failed!" }
        }
        //</editor-fold>
    }

    override fun onShuffleModeEnabledChanged( shuffleModeEnabled: Boolean ) {
        Preferences.PLAYER_SHUFFLE.value = shuffleModeEnabled

        updateMediaControl()
    }

    override fun onRepeatModeChanged( repeatMode: Int ) {
        Preferences.QUEUE_LOOP_TYPE.value = QueueLoopType.from( repeatMode )

        updateMediaControl()
    }

    /**
     * When a song's videoId is no longer playable (YouTube retired/remapped it),
     * search for a replacement by title + artist:
     *   - STRONG match → swap the id in DB + queue silently and resume.
     *   - WEAK match   → emit to [RematchRequests] so the UI can ask the user.
     *   - No candidates → show a toast directing the user to the manual Rematch menu.
     */
    private fun launchRematch( deadMediaItem: MediaItem ) {
        val deadId = deadMediaItem.mediaId
        coroutineScope.launch {
            // Prefer the stored Song (has accurate durationText for confidence scoring).
            // Fall back to constructing from MediaItem metadata if the song isn't in the DB yet.
            val deadSong: Song = Database.songTable.findById( deadId ).first()
                ?: Song(
                    id           = deadId,
                    title        = deadMediaItem.mediaMetadata.title?.toString() ?: return@launch,
                    artistsText  = deadMediaItem.mediaMetadata.artist?.toString(),
                    durationText = null,
                    thumbnailUrl = deadMediaItem.mediaMetadata.artworkUri?.toString()
                )

            logger.i { "Rematch: searching replacement for $deadId (${deadSong.title})" }
            Toaster.i( context.getString( R.string.rematch_searching ) )
            // Exclude the dead id and any id already attempted this session, so two
            // equally-unplayable uploads of the same song can't ping-pong (A→B→A→…).
            // rematchedSongs persists until a song reaches STATE_READY.
            val candidates = SongRematcher.searchCandidates( deadSong )
                .filterNot { it.key == deadId || it.key in rematchedSongs }
            val match      = SongRematcher.bestMatch( deadSong, candidates )

            when {
                match == null -> {
                    logger.w { "Rematch: no candidates found for $deadId" }
                    Toaster.w( context.getString( R.string.rematch_no_candidates ) )
                }

                match.confidence == SongRematcher.Confidence.STRONG -> {
                    val newId = match.item.key
                    logger.i { "Rematch STRONG: swapping $deadId → $newId" }
                    Database.reIdSong( deadId, newId, match.item )
                    withContext( Dispatchers.Main ) {
                        val idx = currentMediaItemIndex
                        replaceMediaItem( idx, match.item.asMediaItem )
                        clearCachedStreamUrlOf( deadId )
                        seekToDefaultPosition()
                        prepare()
                        play()
                    }
                    Toaster.i( context.getString( R.string.rematch_replaced_unavailable_track ) )
                }

                else -> {
                    // WEAK confidence — ask the user to confirm
                    logger.i { "Rematch WEAK: asking user to confirm replacement for $deadId" }
                    RematchRequests.emit(
                        RematchRequests.Request(
                            deadSong   = deadSong,
                            candidates = candidates,
                            onAccepted = { chosen ->
                                val newId = chosen.key
                                coroutineScope.launch {
                                    Database.reIdSong( deadId, newId, chosen )
                                    withContext( Dispatchers.Main ) {
                                        val idx = currentMediaItemIndex
                                        if( getMediaItemAt( idx ).mediaId == deadId ) {
                                            replaceMediaItem( idx, chosen.asMediaItem )
                                            clearCachedStreamUrlOf( deadId )
                                            seekToDefaultPosition()
                                            prepare()
                                            play()
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    override fun onPlayerError( error: PlaybackException ) {
        val rootCause = traverseErrorStack( error )

        if (rootCause is HttpDataSource.InvalidResponseCodeException
            && rootCause.responseCode == 403) {
            val songId = currentMediaItem?.mediaId
            if (songId != null && retried403Songs.add(songId)) {
                logger.i { "Got HTTP 403 for $songId; clearing cached stream url and re-resolving" }
                clearCachedStreamUrlOf(songId)
                seekToDefaultPosition()
                prepare()
                return
            }
            logger.w { "HTTP 403 for ${currentMediaItem?.mediaId} persisted after re-resolution" }
        } else if (rootCause is HttpDataSource.InvalidResponseCodeException) {
            logger.w { "Playback HTTP ${rootCause.responseCode} for ${currentMediaItem?.mediaId}" }
        }

        // ── Auto-rematch for unavailable / unresolvable songs ─────────────────
        if( (rootCause is UnplayableException || rootCause is PlayableFormatNotFoundException)
            && currentMediaItem != null
        ) {
            val mediaItem = currentMediaItem!!
            if( !mediaItem.isLocal && rematchedSongs.add( mediaItem.mediaId ) ) {
                logger.i { "Song unplayable (${rootCause::class.simpleName}); attempting rematch for ${mediaItem.mediaId}" }
                launchRematch( mediaItem )
                // Don't show the error toast yet — launchRematch will either
                // silently fix it or show its own feedback.
                return
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        // ── Recover from a corrupted streaming-cache entry ────────────────────
        // The media3 disk cache is keyed by videoId only, with no discriminator for the
        // resolved format. If an earlier attempt left a partial span of a different stream
        // (e.g. adaptive audio from a teaser-blocked URL) and we now write a different one
        // (e.g. the progressive itag-18 fallback), SimpleCache.commitFile's isFullyLocked
        // assertion throws and playback dies deterministically at the stale span boundary
        // ("plays a few seconds, then stops"; a full download bypasses this cache entirely).
        // Eviction is safe here: the failed load has already torn down, so no writer holds a
        // lock. Re-resolving then caches one consistent stream.
        val cacheErrSongId = currentMediaItem?.mediaId
        if( cacheErrSongId != null
            && currentMediaItem?.isLocal != true
            && isCacheCommitError( error )
            && cacheRecoveredSongs.add( cacheErrSongId )
        ) {
            logger.i { "Streaming cache for $cacheErrSongId corrupted (SimpleCache.commitFile); evicting and re-resolving" }
            clearCachedStreamUrlOf( cacheErrSongId )
            runCatching { streamCache.removeResource( cacheErrSongId ) }
                .onFailure { logger.w( "Failed to evict cache entry for $cacheErrSongId", it ) }
            seekToDefaultPosition()
            prepare()
            return
        }
        // ─────────────────────────────────────────────────────────────────────

        when( rootCause ) {
            is PlayableFormatNotFoundException -> context.getString( R.string.error_couldn_t_find_a_playable_audio_format )
            is NoInternetException -> context.getString( R.string.no_connection )
            is MissingDecipherKeyException -> context.getString( R.string.error_failed_to_decipher_signature )

            else -> rootCause.message ?: context.getString( R.string.error_unknown )
        }.also( ::printErrorMessage )

        if ( Preferences.PLAYBACK_SKIP_ON_ERROR.value && hasNextMediaItem() )
            playNext()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            retried403Songs.clear()
            rematchedSongs.clear()
            cacheRecoveredSongs.clear()
        }
    }

    override fun onEvents( player: Player, events: Player.Events ) {
        if (
            events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING
                    || player.playbackState == Player.STATE_READY
            val intent = Intent(
                if( isBufferingOrReady && player.playWhenReady )
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
                else
                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION
            )
                .putExtra( AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId )
                .putExtra( AudioEffect.EXTRA_PACKAGE_NAME, context.packageName )
                .putExtra( AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC )

            context.sendBroadcast( intent )
        }
    }


    /*
            SharedPreferences listener
     */

    override fun onSharedPreferenceChanged( pref: SharedPreferences, key: String? ) {
        when( key ) {
            Preferences.Key.AUDIO_VOLUME_NORMALIZATION -> {
                if( ::loudnessEnhancer.isInitialized )
                    loudnessEnhancer.enabled = pref.getBoolean(key, false)
                normalizeLoudness()
            }
            Preferences.Key.AUDIO_VOLUME_NORMALIZATION_TARGET -> normalizeLoudness()

            Preferences.Key.AUDIO_SKIP_SILENCE ->  skipSilenceEnabled = pref.getBoolean( key, false )

            Preferences.Key.AUDIO_BASS_BOOSTED -> {
                if( ::bassBoost.isInitialized )
                    bassBoost.enabled = pref.getBoolean(key, false)
                boostLowFrequencies()
            }
            Preferences.Key.AUDIO_BASS_BOOST_LEVEL -> boostLowFrequencies()

            Preferences.Key.AUDIO_REVERB_PRESET -> updateReverb()

            Preferences.Key.QUEUE_LOOP_TYPE ->
                repeatMode = pref.getEnum( key, QueueLoopType.Default ).type

            Preferences.Key.MEDIA_NOTIFICATION_FIRST_ICON,
            Preferences.Key.MEDIA_NOTIFICATION_SECOND_ICON -> updateMediaControl()

            Preferences.Key.ENABLE_PERSISTENT_QUEUE -> {
                val isEnabled = pref.getBoolean(key, false)

                _isPersistentQueueEnabled.update { isEnabled }
                if( !isEnabled )
                    Database.asyncTransaction { queueTable.deleteAll() }
            }
        }
    }

    private inner class ForwardingPlayerImpl : ForwardingPlayer(this@StatefulPlayerImpl) {

        override fun getAvailableCommands(): Player.Commands =
            super.availableCommands.buildUpon().addAllCommands().build()
    }
}
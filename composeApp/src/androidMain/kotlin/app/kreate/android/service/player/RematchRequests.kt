package app.kreate.android.service.player

import app.kreate.database.models.Song
import it.fast4x.innertube.Innertube
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A thin singleton bus that carries "please show a re-match confirmation
 * dialog" requests from the player service to the UI layer.
 *
 * Emitted by [StatefulPlayerImpl] when a song is unplayable and the
 * automatic best-match confidence is too low to swap silently.
 * Consumed by the Player composable.
 */
object RematchRequests {

    data class Request(
        /** The song whose stored videoId is no longer playable. */
        val deadSong: Song,
        /** Pre-fetched candidate list (may be empty if search failed). */
        val candidates: List<Innertube.SongItem>,
        /**
         * Callback the UI must invoke once the user selects a replacement
         * (or null to dismiss without action).
         */
        val onAccepted: (Innertube.SongItem) -> Unit
    )

    private val _flow = MutableSharedFlow<Request>(extraBufferCapacity = 4)

    /** Collect this in the Compose UI to show confirmation dialogs. */
    val flow: SharedFlow<Request> = _flow.asSharedFlow()

    /** Emit a request from the player thread (non-suspending). */
    fun emit( request: Request ) {
        _flow.tryEmit( request )
    }
}

@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters

/**
 * Disable video track selection.
 *
 * The `METHOD_ANDROID` last-resort fallback in
 * [app.kreate.di.resolveInnertubeMedia] returns a *progressive* format (itag 18/22), which is
 * muxed H.264 + AAC. Without this, ExoPlayer selects the video track and starts a hardware
 * video decoder for frames nothing ever draws.
 *
 * Kreate renders no ExoPlayer video anywhere — the one video surface in the app
 * (`it.fast4x.rimusic.ui.screens.player.components.YoutubePlayer`) is a third-party embedded
 * IFrame player with its own pipeline, unaffected by this.
 */
fun audioOnly( base: TrackSelectionParameters ): TrackSelectionParameters =
    base.buildUpon()
        .setTrackTypeDisabled( C.TRACK_TYPE_VIDEO, true )
        .build()

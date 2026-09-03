@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioOnlyTrackSelectionTest {

    /**
     * The METHOD_ANDROID fallback serves muxed H.264 + AAC (itag 18/22). Nothing in the app
     * renders ExoPlayer video, so the video track must never be selected.
     */
    @Test
    fun videoTrackIsDisabled() {
        val result = audioOnly( TrackSelectionParameters.Builder().build() )
        assertTrue( C.TRACK_TYPE_VIDEO in result.disabledTrackTypes )
    }

    @Test
    fun audioTrackStaysEnabled() {
        val result = audioOnly( TrackSelectionParameters.Builder().build() )
        assertFalse( C.TRACK_TYPE_AUDIO in result.disabledTrackTypes )
    }

    /** Must layer onto whatever parameters the player already carries, not replace them. */
    @Test
    fun preservesExistingParameters() {
        val base = TrackSelectionParameters.Builder()
                                           .setPreferredAudioLanguage( "de" )
                                           .build()
        val result = audioOnly( base )
        assertTrue( "de" in result.preferredAudioLanguages )
        assertTrue( C.TRACK_TYPE_VIDEO in result.disabledTrackTypes )
    }
}

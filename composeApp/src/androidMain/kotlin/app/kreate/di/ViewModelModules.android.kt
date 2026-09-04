package app.kreate.di

import app.kreate.android.viewmodel.LocalPlaylistViewModel
import app.kreate.android.viewmodel.YouTubePlaylistViewModel
import app.kreate.android.viewmodel.YoutubeArtistViewModel
import app.kreate.android.viewmodel.home.HomeAlbumsViewModel
import app.kreate.android.viewmodel.home.HomeArtistsViewModel
import app.kreate.android.viewmodel.home.HomeLibraryViewModel
import app.kreate.android.viewmodel.home.OnDeviceSongsViewModel
import app.kreate.android.viewmodel.player.ActionBarNextSongsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


actual val viewModelModule: Module = module {
    viewModelOf( ::YoutubeArtistViewModel )
    viewModelOf( ::YouTubePlaylistViewModel )
    viewModelOf( ::LocalPlaylistViewModel )
    viewModelOf( ::OnDeviceSongsViewModel )
    viewModelOf( ::HomeLibraryViewModel )
    viewModelOf( ::HomeAlbumsViewModel )
    viewModelOf( ::HomeArtistsViewModel )

    viewModel { params ->
        ActionBarNextSongsViewModel( params.get() )
    }
}
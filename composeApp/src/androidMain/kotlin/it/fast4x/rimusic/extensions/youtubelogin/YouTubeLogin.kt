package it.fast4x.rimusic.extensions.youtubelogin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.kreate.android.Preferences
import app.kreate.android.R
import co.touchlab.kermit.Logger
import com.metrolist.innertube.YouTube
import it.fast4x.rimusic.LocalPlayerAwareWindowInsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.knighthat.utils.Toaster

@OptIn(
    DelicateCoroutinesApi::class,
    ExperimentalMaterial3Api::class
)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLogin( onDone: () -> Unit ) {
    var webView: WebView? = null

    // This section is ripped from Metrolist - Full credit to their team
    // Small changes were made in order to make it work with Kreate
    // https://github.com/mostafaalagamy/Metrolist/blob/main/app/src/main/kotlin/com/metrolist/music/ui/screens/LoginScreen.kt
    AndroidView(
        modifier = Modifier.windowInsetsPadding( LocalPlayerAwareWindowInsets.current )
                           .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished( view: WebView, url: String? ) {
                        super.onPageFinished( view, url )

                        if( url?.startsWith("https://music.youtube.com") != true )
                            // Only extract credentials when user is redirected back to YTM page
                            return

                        // Never overwrite a good credential with a blank one. onPageFinished can
                        // fire before `yt.config_` is populated, in which case evaluateJavascript
                        // yields "null"; storing "" would wipe YOUTUBE_VISITOR_DATA, whose default
                        // is a working CHROME_WINDOWS_VISITOR_DATA fallback that PO-token minting
                        // depends on.
                        CookieManager.getInstance()
                                     .getCookie( url )
                                     ?.takeIf( String::isNotBlank )
                                     ?.let { Preferences.YOUTUBE_COOKIES.value = it }
                        YouTube.cookie = Preferences.YOUTUBE_COOKIES.value.takeIf( String::isNotBlank )

                        evaluateJavascript( "window.yt.config_.VISITOR_DATA" ) { result ->
                            result.takeIf { it != "null" }
                                  ?.removeSurrounding("\"")
                                  ?.takeIf( String::isNotBlank )
                                  ?.let { Preferences.YOUTUBE_VISITOR_DATA.value = it }
                            YouTube.visitorData = Preferences.YOUTUBE_VISITOR_DATA.value.takeIf( String::isNotBlank )
                        }
                        evaluateJavascript( "window.yt.config_.DATASYNC_ID" ) { result ->
                            result.takeIf { it != "null" }
                                  ?.removeSurrounding("\"")
                                  ?.substringBefore("||")
                                  ?.takeIf( String::isNotBlank )
                                  ?.let { Preferences.YOUTUBE_SYNC_ID.value = it }
                            YouTube.dataSyncId = Preferences.YOUTUBE_SYNC_ID.value.takeIf( String::isNotBlank )
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            YouTube.accountInfo()
                                   .onFailure { err ->
                                       Logger.e( "", err, "YouTubeLogin" )
                                       Toaster.e( R.string.error_failed_to_acquire_account_info )
                                   }
                                   .onSuccess {
                                       withContext( Dispatchers.Main ) {
                                           Preferences.YOUTUBE_ACCOUNT_NAME.value = it.name
                                           Preferences.YOUTUBE_ACCOUNT_EMAIL.value = it.email.orEmpty()
                                           Preferences.YOUTUBE_SELF_CHANNEL_HANDLE.value = it.channelHandle.orEmpty()
                                           Preferences.YOUTUBE_ACCOUNT_AVATAR.value = it.thumbnailUrl.orEmpty()
                                       }
                                   }
                        }

                        onDone()
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                webView = this
                loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
            }
        }
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}


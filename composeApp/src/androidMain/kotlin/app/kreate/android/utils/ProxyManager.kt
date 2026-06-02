package app.kreate.android.utils

import app.kreate.android.Preferences
import app.kreate.android.R
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.knighthat.utils.Toaster
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ProxyManager"

object ProxyManager {

    private val checking = AtomicBoolean(false)

    /** Build a [Proxy] from current preferences without probing the network. */
    private fun buildCurrentProxy(): Proxy? {
        if (!Preferences.IS_PROXY_ENABLED.value) return null
        val host = Preferences.PROXY_HOST.value.takeIf { it.isNotBlank() } ?: return null
        return Proxy(
            Preferences.PROXY_SCHEME.value,
            InetSocketAddress(host, Preferences.PROXY_PORT.value)
        )
    }

    /**
     * OkHttp ProxySelector consulted on every new connection.
     *
     * Routing is **prefs-driven and fail-closed**: when the proxy is enabled, every
     * connection uses the configured proxy — never direct. [connectFailed] does NOT
     * downgrade to [Proxy.NO_PROXY] so that a transient failure cannot leak a
     * direct-egress request while the proxy is enabled.
     */
    val proxySelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> =
            listOf(buildCurrentProxy() ?: Proxy.NO_PROXY)

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            if (Preferences.IS_PROXY_ENABLED.value) {
                Logger.w(TAG) { "Proxy connection to $uri failed — scheduling health recheck" }
                scheduleCheck()
                // Do NOT fall back to direct: traffic fails closed when proxy is enabled.
            }
        }
    }

    /** Start watching network availability and do an initial proxy probe. */
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            ConnectivityUtils.isAvailable.collect { available ->
                if (available) scheduleCheck()
            }
        }
    }

    /**
     * Schedule a non-blocking background proxy probe.
     * Only drives health-logging — routing is determined by prefs, not this probe.
     */
    fun scheduleCheck() {
        if (!checking.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reachable = buildAndProbeProxy() != null
                Logger.i(TAG) { "Proxy health check: ${if (reachable) "reachable" else "unreachable"}" }
            } finally {
                checking.set(false)
            }
        }
    }

    /**
     * Blocking probe used by the "Test proxy" button.
     * Returns true if the proxy is reachable; shows a failure toast otherwise.
     * Routing is unaffected — it already uses the proxy when enabled.
     */
    fun recheckWithFeedback(): Boolean {
        val reachable = buildAndProbeProxy() != null
        if (!reachable && Preferences.IS_PROXY_ENABLED.value)
            Toaster.w(R.string.error_failed_to_verify_proxy)
        return reachable
    }

    private fun buildAndProbeProxy(): Proxy? {
        if (!Preferences.IS_PROXY_ENABLED.value) return null
        val host = Preferences.PROXY_HOST.value.takeIf { it.isNotBlank() } ?: return null
        val proxy = Proxy(
            Preferences.PROXY_SCHEME.value,
            InetSocketAddress(host, Preferences.PROXY_PORT.value)
        )
        return runCatching {
            OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(3, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build()
                .newCall(Request.Builder().head().url("https://www.youtube.com/generate_204").build())
                .execute()
                .use { if (it.isSuccessful) proxy else null }
        }.onFailure { err ->
            Logger.e(err, TAG) { "Proxy probe failed for $proxy" }
        }.getOrNull()
    }
}

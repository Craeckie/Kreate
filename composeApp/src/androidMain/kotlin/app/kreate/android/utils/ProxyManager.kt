package app.kreate.android.utils

import androidx.core.content.edit
import app.kreate.android.Preferences
import app.kreate.android.R
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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

    private val json = Json { ignoreUnknownKeys = true }

    /** The configured proxy list, decoded from [Preferences.PROXY_LIST]. */
    fun list(): List<ProxyEntry> =
        runCatching { json.decodeFromString<List<ProxyEntry>>(Preferences.PROXY_LIST.value) }
            .getOrDefault(emptyList())

    /** Replace the configured proxy list, clamping the active index if it shrank out of bounds. */
    fun setList(entries: List<ProxyEntry>) {
        Preferences.PROXY_LIST.value = json.encodeToString(entries)
        if (Preferences.PROXY_ACTIVE_INDEX.value >= entries.size)
            Preferences.PROXY_ACTIVE_INDEX.value = 0
    }

    fun activeIndex(): Int = Preferences.PROXY_ACTIVE_INDEX.value

    fun addEntry(entry: ProxyEntry) = setList(list() + entry)

    /** Remove [entry], resetting the active pointer to 0 if it was the currently active one. */
    fun removeEntry(entry: ProxyEntry) {
        val current = list()
        val wasActive = current.indexOf(entry) == activeIndex()
        setList(current - entry)
        if (wasActive) Preferences.PROXY_ACTIVE_INDEX.value = 0
    }

    /** The current sticky proxy, or `null` when disabled or no entries are configured. */
    fun currentProxy(): Proxy? {
        if (!Preferences.IS_PROXY_ENABLED.value) return null
        return list().getOrNull(activeIndex())?.toProxy()
    }

    /**
     * Advance the sticky active proxy past [from] (a no-op unless [from] still matches the
     * current active index, so concurrent failures on the same proxy collapse into a single
     * switch). Returns the resulting active index.
     */
    fun switchToNextProxy(from: Int): Int {
        val rotation = ProxyRotation(list(), activeIndex()).next(from)
        if (rotation.activeIndex != Preferences.PROXY_ACTIVE_INDEX.value) {
            Logger.i(TAG) { "Switching active proxy: index $from -> ${rotation.activeIndex}" }
            Preferences.PROXY_ACTIVE_INDEX.value = rotation.activeIndex
        }
        return rotation.activeIndex
    }

    /**
     * OkHttp ProxySelector consulted on every new connection.
     *
     * Routing is **prefs-driven and fail-closed**: when the proxy is enabled, every
     * connection uses the active configured proxy — never direct. [connectFailed] does NOT
     * downgrade to [Proxy.NO_PROXY] so that a transient failure cannot leak a
     * direct-egress request while the proxy is enabled; it advances the sticky pointer instead.
     */
    val proxySelector: ProxySelector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> =
            listOf(currentProxy() ?: Proxy.NO_PROXY)

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            if (!Preferences.IS_PROXY_ENABLED.value) return

            Logger.w(TAG) { "Proxy connection to $uri failed — scheduling health recheck" }
            scheduleCheck()
            // Do NOT fall back to direct: traffic fails closed when proxy is enabled.

            indexOf(sa)?.let { switchToNextProxy(it) }
        }
    }

    private fun indexOf(sa: SocketAddress?): Int? {
        val inet = sa as? InetSocketAddress ?: return null
        val index = list().indexOfFirst { it.host == inet.hostString && it.port == inet.port }
        return index.takeIf { it >= 0 }
    }

    /**
     * Start watching network availability, do an initial proxy probe, and — once, the first
     * time this runs after the multi-proxy list was introduced — migrate a legacy single
     * proxy setting into it.
     */
    fun initialize() {
        migrateLegacyProxyIfNeeded()

        CoroutineScope(Dispatchers.IO).launch {
            ConnectivityUtils.isAvailable.collect { available ->
                if (available) scheduleCheck()
            }
        }
    }

    /**
     * One-shot migration from the legacy single-proxy prefs ([Preferences.PROXY_HOST] et al.)
     * into [Preferences.PROXY_LIST]. Gated on the **key being absent**, not on the list being
     * empty — checking `== "[]"` would resurrect the legacy proxy every launch after the user
     * deletes all entries, since accessing [Preferences.PROXY_LIST] itself writes the "[]"
     * default once and never after. [Preferences.PROXY_HOST] is cleared once migrated so this
     * can never fire twice.
     */
    private fun migrateLegacyProxyIfNeeded() {
        val prefs = Preferences.preferences
        if (prefs.contains(Preferences.Key.PROXY_LIST)) return

        val legacyHost = prefs.getString(Preferences.Key.PROXY_HOST, "").orEmpty()
        if (legacyHost.isBlank()) return

        val legacyScheme = prefs.getString(Preferences.Key.PROXY_SCHEME, Proxy.Type.HTTP.name)
            ?: Proxy.Type.HTTP.name
        val legacyPort = prefs.getInt(Preferences.Key.PROXY_PORT, 1080)

        setList(listOf(ProxyEntry(scheme = legacyScheme, host = legacyHost, port = legacyPort)))
        Logger.i(TAG) { "Migrated legacy single proxy setting into the proxy list" }

        prefs.edit { putString(Preferences.Key.PROXY_HOST, "") }
    }

    /**
     * Schedule a non-blocking background proxy probe.
     * Only drives health-logging — routing is determined by prefs, not this probe.
     */
    fun scheduleCheck() {
        if (!checking.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reachable = currentProxy()?.let(::probe) == true
                Logger.i(TAG) { "Proxy health check: ${if (reachable) "reachable" else "unreachable"}" }
            } finally {
                checking.set(false)
            }
        }
    }

    /**
     * Blocking probe used by the "Test proxy" button, over the currently active proxy.
     * Returns true if the proxy is reachable; shows a failure toast otherwise.
     * Routing is unaffected — it already uses the proxy when enabled.
     */
    fun recheckWithFeedback(): Boolean {
        val proxy = currentProxy()
        val reachable = proxy?.let(::probe) == true
        if (!reachable && Preferences.IS_PROXY_ENABLED.value)
            Toaster.w(R.string.error_failed_to_verify_proxy)
        return reachable
    }

    /** Probe every configured entry's reachability, independent of the sticky-switch logic. */
    fun probeAll(): Map<ProxyEntry, Boolean> =
        list().associateWith { probe(it.toProxy()) }

    private fun probe(proxy: Proxy): Boolean =
        runCatching {
            OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(3, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build()
                .newCall(Request.Builder().head().url("https://www.youtube.com/generate_204").build())
                .execute()
                .use { it.isSuccessful }
        }.onFailure { err ->
            Logger.e(err, TAG) { "Proxy probe failed for $proxy" }
        }.getOrDefault(false)
}

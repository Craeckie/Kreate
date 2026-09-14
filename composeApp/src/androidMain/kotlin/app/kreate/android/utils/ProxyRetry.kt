package app.kreate.android.utils

import java.io.IOException

/**
 * Marks a resolver-chain failure as "this proxy is exhausted" rather than a fatal/unrelated
 * error — thrown by [app.kreate.di]'s `fallbackOrThrow` for the terminal branches that are
 * plausibly caused by the active proxy being blocked, so [retryAcrossProxies] knows to switch
 * proxies and retry rather than surface the failure immediately.
 */
class ProxyExhaustedException(override val cause: Throwable) : IOException(cause.message, cause)

/**
 * Runs [attempt] and, on [ProxyExhaustedException], switches to the next configured proxy and
 * retries — up to [proxyCount] attempts total, so it always terminates even if [switch] lands
 * on an index this call already tried (e.g. because a concurrent `connectFailed` moved the
 * pointer independently). On exhaustion, or when a retry offers nothing new, the *original*
 * cause is rethrown so callers see what they always saw, not the marker.
 *
 * Deliberately bounded by an attempt counter rather than the visited-index set alone: bounding
 * on the set size only would spin forever if [switch] keeps landing back on an already-visited
 * index.
 */
internal suspend fun <T> retryAcrossProxies(
    proxyCount: Int,
    activeIndex: () -> Int,
    switch: (from: Int) -> Int,
    attempt: suspend () -> T
): T {
    val visited = mutableSetOf(activeIndex())
    var attempts = 0

    while (true) {
        attempts++
        try {
            return attempt()
        } catch (e: ProxyExhaustedException) {
            if (proxyCount < 2 || attempts >= proxyCount) throw e.cause

            var switchedToNew = false
            var switchAttempts = 0
            while (switchAttempts < proxyCount) {
                val newIndex = switch(activeIndex())
                switchAttempts++
                if (visited.add(newIndex)) {
                    switchedToNew = true
                    break
                }
            }
            if (!switchedToNew) throw e.cause
        }
    }
}

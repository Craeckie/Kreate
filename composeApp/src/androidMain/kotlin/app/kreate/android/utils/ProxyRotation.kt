package app.kreate.android.utils

/**
 * Pure rotation core for a sticky "active proxy" pointer into a configured [entries] list.
 *
 * Kept free of Koin/Android dependencies (SharedPreferences, DI) so it can be unit-tested
 * directly; [ProxyManager] is the only caller and is responsible for persisting the result.
 */
data class ProxyRotation(
    val entries: List<ProxyEntry>,
    val activeIndex: Int
) {

    val active: ProxyEntry?
        get() = entries.getOrNull(activeIndex)

    /**
     * Advance to the next entry, wrapping around. No-op when fewer than 2 entries are
     * configured, or when [from] no longer matches [activeIndex] — the latter means a
     * concurrent switch already moved the pointer, so this call collapses into that one
     * instead of double-advancing.
     */
    fun next(from: Int): ProxyRotation {
        if (entries.size < 2 || from != activeIndex) return this
        return copy(activeIndex = (activeIndex + 1) % entries.size)
    }

    /** Replace the entry list, clamping [activeIndex] so it stays in bounds. */
    fun withEntries(list: List<ProxyEntry>): ProxyRotation {
        val clampedIndex = if (list.isEmpty()) 0 else activeIndex.coerceIn(0, list.size - 1)
        return ProxyRotation(list, clampedIndex)
    }
}

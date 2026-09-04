package app.kreate.di

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches a value per key and guarantees that only **one** caller runs the expensive producer
 * for a given key at a time. Everyone else waits and reuses that result.
 *
 * A plain `ConcurrentHashMap` is not enough: reading it, finding nothing, and then producing is
 * a check-then-act race. A field logcat on 2026-09-03 caught the consequence — the player and
 * the downloader both missed the stream-url cache for one song, both ran the full
 * `ANDROID_VR -> IOS -> ANDROID` chain, and both then fetched the same 5,622,245-byte file.
 *
 * Locking is per key, never global: resolving song A must not block song B.
 */
class SingleFlight<K : Any, V : Any> {

    private val values = ConcurrentHashMap<K, V>()
    private val locks = ConcurrentHashMap<K, Mutex>()

    /**
     * Return the cached value for [key] when one exists and [isValid] accepts it, otherwise run
     * [produce] and cache the result.
     *
     * @param isValid rejects a stale entry (an expired stream url), forcing a fresh [produce]
     */
    suspend fun get( key: K, isValid: (V) -> Boolean, produce: suspend () -> V ): V {
        // Fast path: a valid cached value needs no lock at all.
        values[key]?.takeIf( isValid )?.let { return it }

        return locks.computeIfAbsent( key ) { Mutex() }.withLock {
            // Someone may have produced it while we waited for the lock.
            values[key]?.takeIf( isValid )?.let { return@withLock it }

            produce().also { values[key] = it }
        }
    }

    /** Drop the cached value for [key]. Returns `true` if there was one. */
    fun invalidate( key: K ): Boolean = values.remove( key ) != null
}

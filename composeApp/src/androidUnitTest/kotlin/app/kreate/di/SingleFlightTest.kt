package app.kreate.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the defect seen in a field logcat on 2026-09-03: one song produced five
 * `/youtubei/v1/player` resolutions and **two** full 5,622,245-byte downloads of the same
 * audio, because the player and the downloader both missed the stream-url cache and both ran
 * the whole VR -> IOS -> ANDROID chain concurrently.
 */
class SingleFlightTest {

    /** Producer slow enough that every caller is inside the call before the first finishes. */
    private suspend fun slowProduce( counter: AtomicInteger, value: String ): String {
        counter.incrementAndGet()
        delay( 200 )
        return value
    }

    @Test
    fun concurrentCallersForOneKeyProduceOnlyOnce() = runBlocking {
        val flight = SingleFlight<String, String>()
        val produced = AtomicInteger( 0 )

        val results = ( 1..8 ).map {
            async( Dispatchers.Default ) {
                flight.get( "songA", isValid = { true } ) { slowProduce( produced, "url-A" ) }
            }
        }.awaitAll()

        assertEquals( 1, produced.get(), "the expensive resolution must run exactly once" )
        assertTrue( results.all { it == "url-A" }, "every caller must get the same result" )
    }

    @Test
    fun cachedValidValueIsReusedWithoutProducing() = runBlocking {
        val flight = SingleFlight<String, String>()
        val produced = AtomicInteger( 0 )

        repeat( 3 ) {
            flight.get( "songA", isValid = { true } ) { slowProduce( produced, "url-A" ) }
        }

        assertEquals( 1, produced.get(), "a still-valid cached value must not be re-produced" )
    }

    @Test
    fun invalidCachedValueIsProducedAgain() = runBlocking {
        val flight = SingleFlight<String, String>()
        val produced = AtomicInteger( 0 )

        // isValid=false stands in for an expired stream url.
        repeat( 3 ) {
            flight.get( "songA", isValid = { false } ) { slowProduce( produced, "url-A" ) }
        }

        assertEquals( 3, produced.get(), "an invalid cached value must be re-resolved" )
    }

    @Test
    fun invalidateForcesTheNextCallToProduce() = runBlocking {
        val flight = SingleFlight<String, String>()
        val produced = AtomicInteger( 0 )

        flight.get( "songA", isValid = { true } ) { slowProduce( produced, "url-A" ) }
        assertTrue( flight.invalidate( "songA" ), "invalidate must report it removed a value" )
        flight.get( "songA", isValid = { true } ) { slowProduce( produced, "url-A" ) }

        assertEquals( 2, produced.get(), "invalidate must force a fresh resolution" )
        assertTrue( !flight.invalidate( "songB" ), "invalidate must report false for an absent key" )
    }

    /**
     * Different songs must resolve in parallel. A single global lock would satisfy every test
     * above while serialising the whole app, so this pins per-key locking specifically: each
     * producer blocks until the other has also started, which can only happen concurrently.
     */
    @Test
    fun differentKeysResolveInParallel() = runBlocking {
        val flight = SingleFlight<String, String>()
        val bothStarted = CountDownLatch( 2 )

        val results = listOf( "songA", "songB" ).map { key ->
            async( Dispatchers.Default ) {
                flight.get( key, isValid = { true } ) {
                    bothStarted.countDown()
                    assertTrue(
                        bothStarted.await( 5, TimeUnit.SECONDS ),
                        "resolutions for different keys were serialised"
                    )
                    "url-$key"
                }
            }
        }.awaitAll()

        assertEquals( listOf( "url-songA", "url-songB" ), results )
    }
}

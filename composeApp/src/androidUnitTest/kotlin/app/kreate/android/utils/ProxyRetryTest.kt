package app.kreate.android.utils

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProxyRetryTest {

    private class RootCause(message: String) : RuntimeException(message)

    @Test
    fun `switches once and succeeds on the second proxy`() = runBlocking {
        var attempts = 0
        var switches = 0
        var active = 0

        val result = retryAcrossProxies(
            proxyCount = 2,
            activeIndex = { active },
            switch = { from -> switches++; active = (from + 1) % 2; active }
        ) {
            attempts++
            if (attempts == 1) throw ProxyExhaustedException(RootCause("first proxy blocked"))
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(2, attempts)
        assertEquals(1, switches)
    }

    @Test
    fun `all proxies failing makes exactly N attempts then rethrows the original cause`() = runBlocking {
        val proxyCount = 3
        var attempts = 0
        var active = 0
        val cause = RootCause("always blocked")

        val thrown = assertFailsWith<RootCause> {
            retryAcrossProxies(
                proxyCount = proxyCount,
                activeIndex = { active },
                switch = { from -> active = (from + 1) % proxyCount; active }
            ) {
                attempts++
                throw ProxyExhaustedException(cause)
            }
        }

        assertSame(cause, thrown)
        assertEquals(proxyCount, attempts)
    }

    /**
     * Simulates a concurrent `connectFailed` bouncing the pointer between two indices only, so
     * a third configured proxy is never reached. Termination must come from the attempt
     * counter, not from ever finding an unvisited index — bounding on the visited-index set
     * alone was an earlier draft's bug (it would spin forever here).
     */
    @Test
    fun `terminates when a concurrent switch keeps landing on a visited index`() = runBlocking {
        val proxyCount = 3
        var attempts = 0
        var active = 0
        val cause = RootCause("blocked")

        val thrown = assertFailsWith<RootCause> {
            retryAcrossProxies(
                proxyCount = proxyCount,
                activeIndex = { active },
                switch = { active = if (active == 0) 1 else 0; active }
            ) {
                attempts++
                throw ProxyExhaustedException(cause)
            }
        }

        assertSame(cause, thrown)
        assertTrue(attempts <= proxyCount)
    }

    @Test
    fun `a non-proxy-exhausted failure is rethrown immediately without switching`() = runBlocking {
        var attempts = 0
        var switches = 0
        val cause = RootCause("login required")

        val thrown = assertFailsWith<RootCause> {
            retryAcrossProxies(
                proxyCount = 3,
                activeIndex = { 0 },
                switch = { switches++; 0 }
            ) {
                attempts++
                throw cause
            }
        }

        assertSame(cause, thrown)
        assertEquals(1, attempts)
        assertEquals(0, switches)
    }

    @Test
    fun `fewer than two proxies rethrows immediately without switching`() = runBlocking {
        var attempts = 0
        var switches = 0
        val cause = RootCause("only one proxy")

        val thrown = assertFailsWith<RootCause> {
            retryAcrossProxies(
                proxyCount = 1,
                activeIndex = { 0 },
                switch = { switches++; 0 }
            ) {
                attempts++
                throw ProxyExhaustedException(cause)
            }
        }

        assertSame(cause, thrown)
        assertEquals(1, attempts)
        assertEquals(0, switches)
    }
}

package app.kreate.di

import it.fast4x.rimusic.service.LoginRequiredException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the loop seen in a field logcat on 2026-09-13: `ANDROID` progressive's LOGIN_REQUIRED
 * used to re-enter `IOS` (because the retry-once-with-a-pot branch checked
 * `poToken == null` but not `method == METHOD_IOS`), which then fell back to `ANDROID` again —
 * 241 iterations in 80s with no depth guard. [nextFallback] is the pure decision table that
 * replaced that ad-hoc `if`/`catch` logic; every rung's next hop is asserted here so the chain's
 * termination no longer depends on getting a scattered set of conditionals right.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InnertubeFallbackPolicyTest {

    private val loginRequired = RungFailure.Threw(LoginRequiredException("bot check"))
    private val otherException = RungFailure.Threw(RuntimeException("boom"))
    private val urlRejected = RungFailure.UrlRejected

    @Test
    fun vrLoginRequiredWithoutPotRetriesVrWithPot() {
        assertEquals(
            Fallback(METHOD_ANDROID_VR, withPoToken = true),
            nextFallback(METHOD_ANDROID_VR, loginRequired, hadPoToken = false)
        )
    }

    @Test
    fun vrLoginRequiredWithPotFallsToIos() {
        assertEquals(
            Fallback(METHOD_IOS, withPoToken = true),
            nextFallback(METHOD_ANDROID_VR, loginRequired, hadPoToken = true)
        )
    }

    @Test
    fun vrOtherExceptionFallsToIos() {
        assertEquals(
            Fallback(METHOD_IOS, withPoToken = true),
            nextFallback(METHOD_ANDROID_VR, otherException, hadPoToken = false)
        )
    }

    @Test
    fun vrUrlRejectedFallsToIos() {
        assertEquals(
            Fallback(METHOD_IOS, withPoToken = true),
            nextFallback(METHOD_ANDROID_VR, urlRejected, hadPoToken = false)
        )
    }

    @Test
    fun iosLoginRequiredWithoutPotRetriesIosWithPot() {
        assertEquals(
            Fallback(METHOD_IOS, withPoToken = true),
            nextFallback(METHOD_IOS, loginRequired, hadPoToken = false)
        )
    }

    @Test
    fun iosLoginRequiredWithPotFallsToAndroid() {
        assertEquals(
            Fallback(METHOD_ANDROID, withPoToken = false),
            nextFallback(METHOD_IOS, loginRequired, hadPoToken = true)
        )
    }

    @Test
    fun iosOtherExceptionFallsToAndroid() {
        assertEquals(
            Fallback(METHOD_ANDROID, withPoToken = false),
            nextFallback(METHOD_IOS, otherException, hadPoToken = false)
        )
    }

    @Test
    fun iosUrlRejectedFallsToAndroid() {
        assertEquals(
            Fallback(METHOD_ANDROID, withPoToken = false),
            nextFallback(METHOD_IOS, urlRejected, hadPoToken = false)
        )
    }

    /** THE BUG: today this re-enters IOS instead of terminating the chain. */
    @Test
    fun androidLoginRequiredWithoutPotTerminatesChain() {
        assertNull(nextFallback(METHOD_ANDROID, loginRequired, hadPoToken = false))
    }

    @Test
    fun androidLoginRequiredWithPotTerminatesChain() {
        assertNull(nextFallback(METHOD_ANDROID, loginRequired, hadPoToken = true))
    }

    @Test
    fun androidUrlRejectedTerminatesChain() {
        assertNull(nextFallback(METHOD_ANDROID, urlRejected, hadPoToken = false))
    }

    @Test
    fun androidOtherExceptionTerminatesChain() {
        assertNull(nextFallback(METHOD_ANDROID, otherException, hadPoToken = false))
    }

    @Test
    fun chainTerminatesEvenWhenEveryRungReportsLoginRequired() {
        var method = METHOD_ANDROID_VR
        var hadPoToken = false
        var steps = 0

        while (true) {
            val next = nextFallback(method, loginRequired, hadPoToken) ?: break
            steps++
            assertTrue(steps <= 4, "fallback chain did not terminate within 4 hops")
            hadPoToken = next.method == method || next.withPoToken
            method = next.method
        }
    }
}

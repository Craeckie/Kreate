package app.kreate.di

import it.fast4x.rimusic.service.LoginRequiredException
import it.fast4x.rimusic.service.UnplayableException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.ConnectException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A field log on 2026-10-29 showed every rung (VISIONOS, VISIONOS+pot, IOS+pot, ANDROID) failing
 * with LOGIN_REQUIRED "Sign in to confirm you're not a bot" — an IP flag that another exit fixes —
 * yet the proxy rotation never kicked in, because only an [IOException] counted as "maybe the
 * proxy". [isProxyPlausible] is that terminal decision; [isBotCheck] picks the UI message.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProxyPlausibleTest {

    @Test
    fun ioExceptionIsProxyPlausible() {
        assertTrue(isProxyPlausible(IOException("reset")))
        assertTrue(isProxyPlausible(ConnectException("proxy refused")))
    }

    @Test
    fun botCheckLoginRequiredIsProxyPlausible() {
        assertTrue(isProxyPlausible(LoginRequiredException("Sign in to confirm you’re not a bot")))
    }

    /** The reason is localised via `hl`, so any LOGIN_REQUIRED counts, age gate included. */
    @Test
    fun ageGateLoginRequiredIsProxyPlausible() {
        assertTrue(isProxyPlausible(LoginRequiredException("Sign in to confirm your age")))
        assertTrue(isProxyPlausible(LoginRequiredException(null)))
    }

    @Test
    fun unplayableIsNotProxyPlausible() {
        assertFalse(isProxyPlausible(UnplayableException("This video is unavailable")))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun missingFieldIsNotProxyPlausible() {
        assertFalse(isProxyPlausible(MissingFieldException("streamingData", "PlayerResponse")))
    }

    @Test
    fun botCheckIsRecognisedCaseInsensitively() {
        assertTrue(isBotCheck(LoginRequiredException("Sign in to confirm you’re not a bot")))
        assertTrue(isBotCheck(LoginRequiredException("Melde dich an, um zu bestätigen, dass du kein Bot bist")))
    }

    @Test
    fun otherErrorsAreNotBotChecks() {
        assertFalse(isBotCheck(LoginRequiredException("Sign in to confirm your age")))
        assertFalse(isBotCheck(LoginRequiredException(null)))
        assertFalse(isBotCheck(UnplayableException("not a bot")))
        assertFalse(isBotCheck(null))
    }
}

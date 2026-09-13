package app.kreate.di

import it.fast4x.rimusic.service.LoginRequiredException


internal const val METHOD_VISIONOS = 0
internal const val METHOD_ANDROID = 1
internal const val METHOD_IOS = 2

internal fun methodName(method: Int): String = when (method) {
    METHOD_VISIONOS -> "VISIONOS"
    METHOD_ANDROID  -> "ANDROID"
    METHOD_IOS      -> "IOS"
    else            -> "unknown($method)"
}

/** Why the current rung did not produce a playable url. */
internal sealed interface RungFailure {
    /** The player response was fine but [validateStreamUrl] rejected the url (teaser-block). */
    data object UrlRejected : RungFailure
    /** The rung threw; [cause] is the exception. */
    data class Threw(val cause: Throwable) : RungFailure
}

/** The next rung to try. [withPoToken] = attach a PO token (generate one if the caller has none). */
internal data class Fallback(val method: Int, val withPoToken: Boolean)

/**
 * Pure fallback table for the resolver chain. Returns `null` when the chain is exhausted and the
 * caller must throw. Terminates by construction: ANDROID never returns a next rung, and the only
 * self-retry (same method, with a PO token) is offered exactly once, when [hadPoToken] is false.
 */
internal fun nextFallback(method: Int, failure: RungFailure, hadPoToken: Boolean): Fallback? {
    val loginRequired = failure is RungFailure.Threw && failure.cause is LoginRequiredException
    return when (method) {
        METHOD_VISIONOS -> when {
            loginRequired && !hadPoToken -> Fallback(METHOD_VISIONOS, withPoToken = true)
            else                         -> Fallback(METHOD_IOS, withPoToken = true)
        }
        METHOD_IOS -> when {
            loginRequired && !hadPoToken -> Fallback(METHOD_IOS, withPoToken = true)
            else                         -> Fallback(METHOD_ANDROID, withPoToken = false)
        }
        else -> null   // ANDROID progressive is the last rung, whatever went wrong
    }
}

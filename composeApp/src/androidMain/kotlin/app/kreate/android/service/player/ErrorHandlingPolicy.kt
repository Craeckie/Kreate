package app.kreate.android.service.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import java.io.IOException


@UnstableApi
class ErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun isEligibleForFallback(exception: IOException): Boolean =
        exception !is HttpDataSource.InvalidResponseCodeException
}
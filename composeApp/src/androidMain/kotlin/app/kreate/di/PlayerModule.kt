@file:androidx.media3.common.util.UnstableApi

package app.kreate.di

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastFilter
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import app.kreate.android.Preferences
import app.kreate.android.R
import com.dd3boh.outertune.utils.potoken.PoTokenGenerator
import app.kreate.android.service.DownloadHelper
import app.kreate.android.service.innertube.AndroidVrStreamHelper
import app.kreate.android.service.player.ErrorHandlingPolicy
import app.kreate.android.service.player.StatefulPlayer
import app.kreate.android.service.player.StatefulPlayerImpl
import app.kreate.android.service.player.VolumeObserver
import app.kreate.android.utils.CharUtils
import app.kreate.android.utils.ConnectivityUtils
import app.kreate.android.utils.innertube.CURRENT_LOCALE
import app.kreate.android.utils.isLocalFile
import app.kreate.database.models.Format
import co.touchlab.kermit.Logger
import com.grack.nanojson.JsonWriter
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parseQueryString
import io.ktor.util.collections.ConcurrentMap
import io.ktor.util.network.UnresolvedAddressException
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.enums.AudioQualityFormat
import it.fast4x.rimusic.service.LoginRequiredException
import it.fast4x.rimusic.service.MissingDecipherKeyException
import it.fast4x.rimusic.service.NoInternetException
import it.fast4x.rimusic.service.PlayableFormatNotFoundException
import it.fast4x.rimusic.service.UnplayableException
import it.fast4x.rimusic.utils.isNetworkAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import me.knighthat.impl.DownloadHelperImpl
import me.knighthat.innertube.Endpoints
import me.knighthat.innertube.Innertube
import me.knighthat.innertube.UserAgents
import me.knighthat.innertube.response.PlayerResponse
import me.knighthat.utils.Toaster
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds


private const val CHUNK_LENGTH = 512 * 1024L     // 512KB
private const val METHOD_ANDROID_VR = 0
private const val METHOD_ANDROID = 1
private const val METHOD_IOS = 2
private const val METHOD_WEB = 3

/** Human-readable name for a `METHOD_*` constant, for diagnostic logs. */
private fun methodName( method: Int ): String = when( method ) {
    METHOD_ANDROID_VR -> "ANDROID_VR"
    METHOD_ANDROID    -> "ANDROID_REEL"
    METHOD_IOS        -> "IOS"
    METHOD_WEB        -> "WEB"
    else              -> "UNKNOWN($method)"
}

/**
 * Next client to try in the fallback chain `ANDROID_VR → ANDROID_REEL → WEB → IOS`.
 *
 * Returns the same [method] for the terminal client ([METHOD_IOS]) to signal the
 * chain is exhausted. ANDROID_VR is the preferred pot-free path; the legacy clients
 * remain as fallback (notably VR cannot serve "made for kids" videos).
 */
private fun nextFallbackMethod( method: Int ): Int = when( method ) {
    METHOD_ANDROID_VR -> METHOD_ANDROID
    METHOD_ANDROID    -> METHOD_WEB
    METHOD_WEB        -> METHOD_IOS
    else              -> METHOD_IOS    // terminal
}

/**
 * Condense a (signed) stream url into the few params worth logging without
 * leaking the full url: client, itag, expiry and whether a PO token is attached.
 */
private fun summarizeStreamUrl( url: String ): String =
    with( parseQueryString( url.substringAfter( '?', "" ) ) ) {
        "c=${this["c"]} itag=${this["itag"]} expire=${this["expire"]} " +
        "pot=${if( contains( "pot" ) ) "yes" else "no"}"
    }

/**
 * Acts as a lock to keep [upsertSongFormat] from starting before
 * [upsertSongInfo] finishes.
 */
private var databaseWorker: Job = Job()

/**
 * Store id of song just added to the database.
 * This is created to reduce load to Room
 */
private val justInserted = AtomicReference("")
private val cachedStreamUrl = ConcurrentMap<String, StreamCache>()
private val jsonParser =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        useArrayPolymorphism = true
        explicitNulls = false
    }
private val client: HttpClient by inject(HttpClient::class.java)
private val context: Context by inject(Context::class.java)
private val poTokenGenerator: PoTokenGenerator by inject(PoTokenGenerator::class.java)
private val logger = Logger.withTag( "dataspec" )

//<editor-fold desc="Database handlers">
/**
 * Reach out to [Endpoints.NEXT] endpoint for song's information.
 *
 * Info includes:
 * - Titles
 * - Artist(s)
 * - Album
 * - Thumbnails
 * - Duration
 *
 * ### If song IS already inside database
 *
 * It'll replace unmodified columns with fetched data
 *
 * ### If song IS NOT already inside database
 *
 * New record will be created and insert into database
 *
 */
private fun upsertSongInfo( context: Context, videoId: String ) {       // Use this to prevent suspension of thread while waiting for response from YT
    // Skip adding if it's just added in previous call
    if( videoId == justInserted.get() || !isNetworkAvailable( context ) )
        return

    logger.v { "fetching and upserting $videoId's information to the database" }

    databaseWorker = CoroutineScope(Dispatchers.IO ).launch {
        Innertube.songBasicInfo( videoId, CURRENT_LOCALE )
            .onSuccess{
                logger.v { "$videoId's information successfully found and parsed" }

                Database.upsert( it )

                logger.d { "$videoId's information successfully upserted to the database" }
            }
            .onFailure {
                logger.e( "failed to upsert $videoId's information to database", it )
                Toaster.e( R.string.error_failed_to_fetch_songs_info )
            }
    }

    // Must not modify [JustInserted] to [upsertSongFormat] let execute later
}

/**
 * Upsert provided format to the database
 */
private fun upsertSongFormat( videoId: String, format: PlayerResponse.StreamingData.Format ) {
    // Skip adding if it's just added in previous call
    if( videoId == justInserted.get() ) return

    logger.v { "upserting format ${format.itag} of song $videoId to the database" }

    CoroutineScope(Dispatchers.IO ).launch {
        // Wait until this job is finish to make sure song's info
        // is in the database before continuing
        databaseWorker.join()

        Database.asyncTransaction {
            formatTable.upsert(
                Format(
                    videoId,
                    format.itag.toInt(),
                    format.mimeType,
                    format.bitrate.toLong(),
                    format.contentLength?.toLong(),
                    format.lastModified.toLong(),
                    format.loudnessDb
                )
            )

            logger.d { "$videoId is successfully upserted to the database" }

            // Format must be added successfully before setting variable
            justInserted.set( videoId )
        }
    }
}
//</editor-fold>
//<editor-fold desc="Extractors">
@Throws(PlayableFormatNotFoundException::class)
private fun extractFormat(
    streamingData: PlayerResponse.StreamingData?,
    audioQualityFormat: AudioQualityFormat,
    connectionMetered: Boolean
): PlayerResponse.StreamingData.Format {
    logger.v { "extracting format with quality $audioQualityFormat and metered connection: $connectionMetered" }

    val sortedAudioFormats =
        streamingData?.adaptiveFormats
            ?.fastFilter {
                it.mimeType.startsWith( "audio" )
            }
            ?.sortedBy(PlayerResponse.StreamingData.Format::bitrate )
            .orEmpty()
    if( sortedAudioFormats.isEmpty() )
        throw PlayableFormatNotFoundException()

    return when( audioQualityFormat ) {
        AudioQualityFormat.High -> sortedAudioFormats.last()
        AudioQualityFormat.Medium -> sortedAudioFormats[sortedAudioFormats.size / 2]
        AudioQualityFormat.Low -> sortedAudioFormats.first()
        AudioQualityFormat.Auto ->
            if ( connectionMetered && Preferences.IS_CONNECTION_METERED.value )
                sortedAudioFormats[sortedAudioFormats.size / 2]
            else
                sortedAudioFormats.last()
    }.also {
        logger.d { "extracted format ${it.itag}" }
    }
}

@Throws(MissingDecipherKeyException::class)
private fun extractStreamUrl( videoId: String, format: PlayerResponse.StreamingData.Format ): String =
    format.signatureCipher?.let { signatureCipher ->
        logger.v { "deobfuscating signature $signatureCipher" }

        val (s, sp, url) = with( parseQueryString( signatureCipher ) ) {
            val signature = this["s"] ?: throw MissingDecipherKeyException("s")
            val signatureParam = this["sp"] ?: throw MissingDecipherKeyException("sp")
            val signatureUrl = this["url"] ?: throw MissingDecipherKeyException("url")
            Triple(
                signature,
                signatureParam,
                URLBuilder(signatureUrl)
            )
        }
        url.parameters[sp] = YoutubeJavaScriptPlayerManager.deobfuscateSignature( videoId, s )
        url.toString()
    } ?: format.url!!
//</editor-fold>
//<editor-fold desc="Validators">
private suspend fun validateStreamUrl( streamUrl: String ): Boolean =
    client.head( "$streamUrl&range=0-$CHUNK_LENGTH" )
          .status
          .also {
              if( it.isSuccess() )
                  logger.d { "Stream url validated successfully" }
              else
                  logger.w { "Stream url validation returns code ${it.value} - ${it.description}" }
          }
          .isSuccess()
//</editor-fold>
//<editor-fold desc="Get response">
@OptIn(ExperimentalSerializationApi::class)
private suspend fun makeStreamCache(
    songId: String,
    isConnectionMetered: Boolean,
    audioQuality: AudioQualityFormat,
    method: Int = METHOD_ANDROID_VR
): StreamCache {
    logger.i { "[${methodName( method )}] resolving stream for \"$songId\" (metered=$isConnectionMetered, quality=$audioQuality)" }

    val cpn = CharUtils.randomString( 12 )
    try {
        //<editor-fold desc="Getting response">
        val (gl, hl) = with( CURRENT_LOCALE ) {
            ContentCountry(regionCode) to Localization(languageCode)
        }
        var webStreamingPoToken: String? = null
        val jsonResponse = when( method ) {
            METHOD_ANDROID_VR -> AndroidVrStreamHelper.getAndroidVrPlayerResponse( gl, hl, songId, cpn )
            METHOD_ANDROID -> YoutubeStreamHelper.getAndroidReelPlayerResponse( gl, hl, songId, cpn )
            METHOD_WEB -> {
                val localResult = poTokenGenerator.getWebClientPoToken(songId)
                    ?: return makeStreamCache( songId, isConnectionMetered, audioQuality, METHOD_IOS )
                val visitorData = poTokenGenerator.sessionIdentifier
                    ?: Preferences.YOUTUBE_VISITOR_DATA.value
                val npResult = org.schabi.newpipe.extractor.services.youtube.PoTokenResult(
                    visitorData,
                    localResult.playerRequestPoToken,
                    localResult.streamingDataPoToken
                )
                val sigTs = runCatching {
                    YoutubeJavaScriptPlayerManager.getSignatureTimestamp(songId)
                }.getOrNull() ?: 0
                webStreamingPoToken = localResult.streamingDataPoToken
                YoutubeStreamHelper.getWebEmbeddedPlayerResponse( hl, gl, songId, cpn, npResult, sigTs )
            }
            else -> YoutubeStreamHelper.getIosPlayerResponse( gl, hl, songId, cpn, null )
        }
        val serializerClass = Class.forName("me.knighthat.internal.response.PlayerResponseImpl$\$serializer")
        val serializerInstance = serializerClass.getDeclaredField("INSTANCE").get(null) as KSerializer<*>
        val response = jsonParser.decodeFromString(
            serializerInstance, JsonWriter.string( jsonResponse )
        ) as PlayerResponse
        //</editor-fold>
        //<editor-fold desc="Verify playability">
        val playabilityStatus = requireNotNull( response.playabilityStatus ) {
            "playabilityStatus is null!"
        }
        when( playabilityStatus.status ) {
            "OK"                -> logger.d { "[${methodName( method )}] playabilityStatus OK for $songId" }
            "LOGIN_REQUIRED"    -> throw LoginRequiredException(playabilityStatus.reason)
            else                -> throw UnplayableException(playabilityStatus.reason)
        }
        //</editor-fold>
        //<editor-fold desc="Extract and validate stream url">
        val format = extractFormat( response.streamingData, audioQuality, isConnectionMetered )
        val streamUrl = extractStreamUrl( songId, format )
        val validateResult = validateStreamUrl( streamUrl )
        logger.i {
            "[${methodName( method )}] $songId itag=${format.itag} mime=${format.mimeType} " +
            "headOk=$validateResult ${summarizeStreamUrl( streamUrl )}"
        }
        //</editor-fold>

        if( !validateResult ) {
            // Treat a failed HEAD probe like any other resolution failure so the
            // single catch block below drives the fallback chain (avoids running
            // the chain twice via recursion inside this try).
            logger.w { "[${methodName( method )}] stream url failed HEAD validation for $songId" }
            throw PlayableFormatNotFoundException()
        }

        upsertSongFormat( songId, format )

        val contentLength = format.contentLength?.toLong() ?: CHUNK_LENGTH
        val expiresInSeconds = response.streamingData?.expiresInSeconds?.toLong() ?: 3_600L
        val expiredAtMs = System.currentTimeMillis() + expiresInSeconds * 1_000L
        return StreamCache(cpn, contentLength, streamUrl, expiredAtMs, webStreamingPoToken)
    } catch( e: Exception ) {
        val next = nextFallbackMethod( method )
        if( next != method ) {
            logger.w( "[${methodName( method )}] failed (${e.javaClass.simpleName}), falling back to ${methodName( next )} for $songId", e )
            return makeStreamCache( songId, isConnectionMetered, audioQuality, next )
        }
        logger.w( "[${methodName( method )}] failed (${e.javaClass.simpleName}); fallback chain exhausted for $songId", e )

        when( e ) {
            is UnknownHostException,
            is UnresolvedAddressException -> {
                // Make sure it's not a temporary network fluctuation
                if( !ConnectivityUtils.isAvailable.value )
                    throw NoInternetException(e)
            }

            // Only show this exception because this needs update
            // Other errors might be because of unsuccessful stream extraction
            is MissingFieldException -> {
                e.message?.also( Toaster::e )
                logger.e( "", e )
            }
        }

        throw e
    }
}

private fun getPlayableUrl( songId: String ): StreamCache = runBlocking( Dispatchers.IO ) {
    logger.v { "Processing $songId" }

    val cache: StreamCache
    if( cachedStreamUrl.contains(songId) ) {
        cache = cachedStreamUrl[songId]!!
        // Handle expired url with 30secs offset
        if( cache.expiredTimeMillis - 30.seconds.inWholeMilliseconds <= System.currentTimeMillis() ) {
            logger.d { "Cached stream url of $songId expired" }

            cachedStreamUrl.remove( songId )
            return@runBlocking getPlayableUrl( songId )
        } else
            logger.d { "Stream url of $songId is cached" }
    } else {
        val connManager = context.getSystemService<ConnectivityManager>()
        val isConnectionMetered = connManager?.isActiveNetworkMetered ?: false
        val audioQuality by Preferences.AUDIO_QUALITY

        cache = makeStreamCache( songId, isConnectionMetered, audioQuality )
        cachedStreamUrl[songId] = cache
    }

    cache
}
//</editor-fold>
//<editor-fold desc="Resolvers">
private fun resolver( queryInChunks: Boolean, vararg cashes: Cache ) =
    ResolvingDataSource.Resolver { dataSpec ->
        if( dataSpec.uri.isLocalFile() ) {
            logger.d { "playing local song: ${dataSpec.uri}" }
            return@Resolver dataSpec
        }

        val songId = dataSpec.uri.toString()
        upsertSongInfo( context, songId )

        // Delay this block until called. Song can be local too
        val position = dataSpec.position
        val length = dataSpec.length.takeIf { it > C.LENGTH_UNSET } ?: CHUNK_LENGTH
        val isCached = cashes.any {
            it.isCached( songId, position, length )
        }
        if( isCached ) {
            logger.v { "Chunk $position - ${position + length} of $songId is cached" }
            // No need to fetch online for already cached data
            dataSpec
        } else {
            val cache = getPlayableUrl( songId )
            val deobUrl = try {
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated( songId, cache.playableUrl )
            } catch( e: Exception ) {
                logger.w( "Throttling parameter deobfuscation failed for $songId, clearing cache and rethrowing", e )
                clearCachedStreamUrlOf(songId)
                throw e
            }
            val potSuffix = cache.streamingPoToken?.let { "&pot=$it" } ?: ""
            val uri = "$deobUrl&cpn=${cache.cpn}$potSuffix".toUri()
            val length = CHUNK_LENGTH.takeIf { queryInChunks } ?: cache.contentLength

            logger.i { "resolved $songId -> cpn=${cache.cpn} ${summarizeStreamUrl( uri.toString() )}" }

            dataSpec.withUri( uri ).subrange( dataSpec.uriPositionOffset, length )
        }
    }
//</editor-fold>

/**
 * Short-circuit function to quickly make a [DataSource.Factory] from
 * designated [cache]
 */
private fun dataSourceFactoryFrom( cache: Cache ): CacheDataSource.Factory =
    CacheDataSource.Factory().setCache( cache )

/**
 * Remove cached url of [songId].
 *
 * @return `true` if song's url was cached, and is deleted, `false` otherwise.
 */
fun clearCachedStreamUrlOf( songId: String ): Boolean =
    cachedStreamUrl.remove( songId ) != null

val playerModule = module {
    single { PoTokenGenerator() }

    // [DefaultDataSource.Factory] with [context] is required to read
    // data from local files.
    // Normal HTTP requests are handled by [OkHttpDataSource.Factory]
    single {
        val engine: OkHttpClient = get()
        DefaultDataSource.Factory(
            get(),
            OkHttpDataSource.Factory( engine )
                .setUserAgent( UserAgents.CHROME_WINDOWS )
        )
    }
    single( DatasourceType.PLAYER ) {
        val cache: Cache = get(CacheType.CACHE)
        val downloadCache: Cache = get(CacheType.DOWNLOAD)
        val defaultDatasource: DefaultDataSource.Factory = get()

        ResolvingDataSource.Factory(
            dataSourceFactoryFrom( downloadCache )
                .setCacheWriteDataSinkFactory( null )
                .setFlags( FLAG_IGNORE_CACHE_ON_ERROR )
                .setUpstreamDataSourceFactory(
                    dataSourceFactoryFrom( cache )
                        .setUpstreamDataSourceFactory( defaultDatasource )
                        .setCacheWriteDataSinkFactory(
                            CacheDataSink.Factory()
                                .setCache( cache )
                                .setFragmentSize( CHUNK_LENGTH )
                        )
                        .setFlags( FLAG_IGNORE_CACHE_ON_ERROR )
                ),
            resolver( true, cache, downloadCache )
        )
    }
    single( DatasourceType.DOWNLOADER ) {
        val downloadCache: Cache = get(CacheType.DOWNLOAD)
        val defaultDatasource: DefaultDataSource.Factory = get()

        ResolvingDataSource.Factory(
            dataSourceFactoryFrom( downloadCache )
                .setUpstreamDataSourceFactory( defaultDatasource )
                .setCacheWriteDataSinkFactory( null ),
            resolver( false, downloadCache )
        )
    }

    singleOf( ::VolumeObserver )
    single<StatefulPlayer> {
        val context: Context = get()
        val resolvingFactory: ResolvingDataSource.Factory = get(DatasourceType.PLAYER)
        val handleAudioFocus by Preferences.AUDIO_SMART_PAUSE_DURING_CALLS

        val renderersFactory = DefaultRenderersFactory(context)
        val datasourceFactory = DefaultMediaSourceFactory(
            resolvingFactory,
            DefaultExtractorsFactory()
        )
        datasourceFactory.setLoadErrorHandlingPolicy( ErrorHandlingPolicy() )
        val audioAttributes = AudioAttributes.Builder()
            .setUsage( C.USAGE_MEDIA )
            .setContentType( C.AUDIO_CONTENT_TYPE_MUSIC )
            .build()

        StatefulPlayerImpl(
            context,
            ExoPlayer.Builder(context)
                .setMediaSourceFactory( datasourceFactory )
                .setRenderersFactory( renderersFactory )
                .setHandleAudioBecomingNoisy( true )
                .setWakeMode( C.WAKE_MODE_NETWORK )
                .setAudioAttributes( audioAttributes, handleAudioFocus )
                .setUsePlatformDiagnostics( false )
                .build()
        )
    }
    @SuppressLint("UnsafeOptInUsageError")
    single<DownloadHelper> {
        val dataSourceFactory: ResolvingDataSource.Factory = get(DatasourceType.DOWNLOADER)
        val downloadCache: Cache = get(CacheType.DOWNLOAD)
        val dbProvider: StandaloneDatabaseProvider = get()

        DownloadHelperImpl(dataSourceFactory, get(), downloadCache, dbProvider)
    }
}

enum class DatasourceType : Qualifier {
    PLAYER, DOWNLOADER;

    override val value: QualifierValue = toString().lowercase()
}

private data class StreamCache(
    val cpn: String,
    val contentLength: Long,
    val playableUrl: String,
    val expiredTimeMillis: Long,
    val streamingPoToken: String? = null
)

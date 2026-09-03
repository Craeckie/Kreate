# Download-State Desync & Audio-Only Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the "downloaded" badge tell the truth (it must reflect bytes on disk, not a stale index row), stop every code path that silently desyncs the two stores, and stop decoding H.264 video during audio-only playback.

**Architecture:** The app has two independent stores of "is this song downloaded": media3's `DownloadIndex` (a SQLite table, what the UI reads) and the `exo_downloads` `SimpleCache` (the bytes, what playback reads). Nothing reconciles them, and nine code paths delete bytes without removing the index row. This plan (1) makes the UI predicate require *both*, so the badge is self-healing; (2) routes every deletion through `DownloadManager`, so the two stores move together; (3) removes the `LeastRecentlyUsedCacheEvictor` from the download cache, which silently evicts finished downloads behind `DownloadManager`'s back; (4) disables video track selection on the ExoPlayer, since the `METHOD_ANDROID` progressive fallback serves muxed H.264+AAC and nothing in the app renders ExoPlayer video.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, AndroidX Media3 1.10.1 (ExoPlayer, `DownloadManager`, `SimpleCache`), Koin DI, Room, JUnit4 + `kotlin.test` + Robolectric 4.16.1 for unit tests.

**Spec:** No separate spec document. The findings this plan implements are reproduced in full in "Background: the two-store problem" below; that section is the spec.

---

## Global Constraints

- **Module:** all changes are in `:composeApp`. Android is the production target; do not touch `desktopMain`/`jvmMain`.
- **Toolchain:** JVM 21. Default dev variant is `githubUniversalProdDebug`.
- **Unit test task:** `./gradlew :composeApp:testGithubUniversalProdDebugUnitTest`
- **Test source set:** `composeApp/src/androidUnitTest/kotlin/...` — JUnit4 + `kotlin.test` assertions (see `it/fast4x/rimusic/utils/AppLifecycleTrackerTest.kt` for house style). Robolectric is available but only Task 4 needs it.
- **Package placement:** per `CLAUDE.md`, new code goes under `app.kreate.*`, not `it.fast4x.rimusic.*` or `me.knighthat.*`.
- **Media3 is `@UnstableApi`:** any new file touching `androidx.media3.datasource.cache.*`, `androidx.media3.exoplayer.*`, or `androidx.media3.common.TrackSelectionParameters` needs `@file:androidx.media3.common.util.UnstableApi` at the top, or the build fails with an opt-in error.
- **This environment auto-commits every edit as `wip: <path>` on `main`.** The explicit commit steps below are still correct — run them; they will fold the `wip:` commits into a real message via `git commit --amend` where noted. Squash any leftover `wip:` commits before pushing.
- **Do not bump `versionCode`/`versionName` or write a changelog** as part of this plan. Releasing is a separate step (see `CLAUDE.md` → "Creating a release").
- **Do not change** the `OFFICIAL_BUILD_PASSPHRASE` hash check, the APK naming block, or `applicationIdSuffix`.

---

## Background: the two-store problem

Read this before Task 1. It is the justification for every task.

| Question | Store consulted | Read by |
|---|---|---|
| "Is it downloaded?" (every UI badge) | media3 `DownloadIndex` (`ExoPlayerDownloads` SQLite table) | `downloadedStateMedia()` at `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/utils/DownloadUtils.kt:34-40`, via `MyDownloadHelper.getDownload(id)` → `DownloadHelperImpl.downloads` StateFlow |
| "Can I play it offline?" | `exo_downloads` `SimpleCache` bytes | the download `CacheDataSource` at the bottom of the media source stack, `composeApp/src/androidMain/kotlin/app/kreate/di/PlayerModule.kt:117` |

`DownloadManager` reads its index once at construction and never re-verifies it against the cache. So once the bytes go missing, the badge lies forever, and playback silently re-resolves the stream from YouTube.

**Paths that delete download-cache bytes without removing the index row** (each already carries a `// FIXME: This is unsafe, use [DownloadService.sendRemoveDownload] instead` comment, except the last two):

1. `me/knighthat/component/ResetCache.kt:59`
2. `me/knighthat/component/song/ResetSongDialog.kt:154`
3. `me/knighthat/component/tab/DeleteSongDialog.kt:58`
4. `me/knighthat/component/tab/HideSongDialog.kt:40` — inherits `downloadCache` from `DeleteSongDialog`, so it does not itself name `CacheType.DOWNLOAD`
5. `app/kreate/android/themed/common/component/menu/ResetSongButton.kt:129`
6. `app/kreate/android/themed/common/component/menu/DeleteSongButton.kt:47`
7. `it/fast4x/rimusic/ui/components/themed/PlayerMenu.kt:80`
8. `it/fast4x/rimusic/ui/components/themed/MediaItemMenu.kt:252`
9. `app/kreate/android/themed/common/component/settings/data/ExoCacheIndicator.kt:23` — `cache.keys.forEach(cache::removeResource)`, wired to the **download** cache's clear button at `DataSettings.kt:214`, and auto-fired by `LaunchedEffect(maxCacheSize) { if (0L == maxCacheSize) indicator.onConfirm() }` at `DataSettings.kt:238-240`
10. `LeastRecentlyUsedCacheEvictor` installed on the download cache by `initCache` (`PlayerModule.kt:47-69`) whenever `EXO_DOWNLOAD_SIZE` is not `Long.MAX_VALUE` — media3 assumes the download cache is owned solely by `DownloadManager`; an evictor deletes finished downloads as new ones arrive.

The exact list of files that name the download cache *and* evict from it is reproducible with:

```bash
cd composeApp/src/androidMain/kotlin && \
  for f in $(grep -rl "CacheType.DOWNLOAD" . --include=*.kt); do \
    grep -q "removeResource" "$f" && echo "${f#./}"; done
```

`AbstractMediaDownloadDialog.kt:37` also calls `removeResource`, but on `CacheType.CACHE` — that one is correct and stays.

**Why video is being decoded:** `resolveInnertubeMedia`'s last-resort `METHOD_ANDROID` fallback picks a *progressive* format (itag 18/22), which is muxed H.264 + AAC (`InnertubeResolvingDataSource.kt:204-226`). Nothing in the project configures a `TrackSelector` (`grep -rn TRACK_TYPE_VIDEO composeApp/src` returns nothing), so ExoPlayer selects and decodes the video track. Field logs confirm `c2.exynos.h264.decoder` starting up. The only actual video UI in the app is `it/fast4x/rimusic/ui/screens/player/components/YoutubePlayer.kt`, which uses a third-party `YouTubePlayerView` (an embedded IFrame player) — a completely separate component, unaffected by ExoPlayer track selection. Disabling `C.TRACK_TYPE_VIDEO` globally on the ExoPlayer is therefore safe.

---

### Task 1: Make the "downloaded" badge require bytes on disk

The badge must be true only when the index says COMPLETED **and** the download cache actually holds the whole resource. This alone makes every existing bad state on users' devices self-heal, without a migration.

The authority for "the whole resource" is the download cache's own `ContentMetadata`, not the Room `formats` table — the cache records the content length itself when the download completes, and `SimpleCache.removeResource` / full LRU eviction drop that metadata along with the spans, so a wiped resource reports `C.LENGTH_UNSET`.

**Files:**
- Create: `composeApp/src/androidMain/kotlin/app/kreate/android/service/DownloadCacheState.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/app/kreate/android/service/DownloadCacheStateTest.kt`
- Modify: `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/utils/DownloadUtils.kt:28-40`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `app.kreate.android.service.DownloadCacheState.isFullyCached(cache: androidx.media3.datasource.cache.Cache, key: String): Boolean` — used by Task 1 only, but Task 2's guard test allowlists nothing new, so keep it here.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/androidUnitTest/kotlin/app/kreate/android/service/DownloadCacheStateTest.kt`:

```kotlin
@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.common.C
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Minimal [Cache] stand-in. Only the three members [DownloadCacheState.isFullyCached]
 * touches are implemented; everything else throws so an accidental new dependency
 * shows up as a loud failure rather than a silent default.
 */
private class FakeCache(
    private val contentLength: Long,
    private val cachedBytes: Long,
    private val throwOnAccess: Boolean = false
) : Cache {

    private inner class FakeMetadata : ContentMetadata {
        override fun get( key: String, defaultValue: ByteArray? ): ByteArray? = defaultValue
        override fun get( key: String, defaultValue: String? ): String? = defaultValue
        override fun get( key: String, defaultValue: Long ): Long =
            if ( key == ContentMetadata.KEY_CONTENT_LENGTH ) contentLength else defaultValue
        override fun contains( key: String ): Boolean = key == ContentMetadata.KEY_CONTENT_LENGTH
    }

    override fun getContentMetadata( key: String ): ContentMetadata {
        if ( throwOnAccess ) throw IllegalStateException( "cache released" )
        return FakeMetadata()
    }

    override fun isCached( key: String, position: Long, length: Long ): Boolean {
        if ( throwOnAccess ) throw IllegalStateException( "cache released" )
        return cachedBytes >= position + length
    }

    private fun nope(): Nothing = throw UnsupportedOperationException( "Not needed for this test" )
    override fun getUid(): Long = nope()
    override fun release() = nope()
    override fun addListener( key: String, listener: Cache.Listener ): NavigableSet<CacheSpan> = nope()
    override fun removeListener( key: String, listener: Cache.Listener ) = nope()
    override fun getCachedSpans( key: String ): NavigableSet<CacheSpan> = nope()
    override fun getKeys(): MutableSet<String> = nope()
    override fun getCacheSpace(): Long = nope()
    override fun startReadWrite( key: String, position: Long, length: Long ): CacheSpan = nope()
    override fun startReadWriteNonBlocking( key: String, position: Long, length: Long ): CacheSpan = nope()
    override fun startFile( key: String, position: Long, length: Long ): File = nope()
    override fun commitFile( file: File, length: Long ) = nope()
    override fun releaseHoleSpan( holeSpan: CacheSpan ) = nope()
    override fun removeResource( key: String ) = nope()
    override fun removeSpan( span: CacheSpan ) = nope()
    override fun getCachedLength( key: String, position: Long, length: Long ): Long = nope()
    override fun getCachedBytes( key: String, position: Long, length: Long ): Long = nope()
    override fun applyContentMetadataMutations( key: String, mutations: ContentMetadataMutations ) = nope()
}

class DownloadCacheStateTest {

    @Test
    fun fullyCachedResourceIsReported() {
        val cache = FakeCache( contentLength = 4_000_000L, cachedBytes = 4_000_000L )
        assertTrue( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** Bytes wiped by removeResource(): the metadata goes with them, so length is UNSET. */
    @Test
    fun wipedResourceIsNotReported() {
        val cache = FakeCache( contentLength = C.LENGTH_UNSET.toLong(), cachedBytes = 0L )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** Partially evicted by an LRU evictor: metadata survives, spans do not cover it. */
    @Test
    fun partiallyEvictedResourceIsNotReported() {
        val cache = FakeCache( contentLength = 4_000_000L, cachedBytes = 512_000L )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** A zero-length resource is never "downloaded". */
    @Test
    fun zeroLengthResourceIsNotReported() {
        val cache = FakeCache( contentLength = 0L, cachedBytes = 0L )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }

    /** A released cache must degrade to false, never propagate the exception into composition. */
    @Test
    fun releasedCacheIsNotReported() {
        val cache = FakeCache( contentLength = 4_000_000L, cachedBytes = 4_000_000L, throwOnAccess = true )
        assertFalse( DownloadCacheState.isFullyCached( cache, "abc" ) )
    }
}
```

Add this import at the top of the test file with the others: `import java.util.NavigableSet`.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.android.service.DownloadCacheStateTest"
```

Expected: compilation failure — `Unresolved reference: DownloadCacheState`.

- [ ] **Step 3: Write the implementation**

Create `composeApp/src/androidMain/kotlin/app/kreate/android/service/DownloadCacheState.kt`:

```kotlin
@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata

/**
 * Answers "does this cache actually hold the whole resource?" — the question the
 * download badge must ask, and the one media3's [androidx.media3.exoplayer.offline.DownloadIndex]
 * cannot answer.
 *
 * The index and the cache are independent stores. A row can say
 * [androidx.media3.exoplayer.offline.Download.STATE_COMPLETED] long after the bytes were
 * removed (see `docs/superpowers/plans/2026-09-03-download-state-desync-and-audio-only-playback.md`),
 * which is why the UI must consult both.
 */
object DownloadCacheState {

    /**
     * `true` only when [cache] holds every byte of [key].
     *
     * The content length comes from the cache's own [ContentMetadata], not from the Room
     * `formats` table: `SimpleCache.removeResource` and a full LRU eviction drop the metadata
     * together with the spans, so a wiped resource reports `C.LENGTH_UNSET` (-1) and fails the
     * `> 0` guard. A partially evicted resource keeps its metadata but fails [Cache.isCached].
     *
     * Never throws — a released or uninitialized cache reports `false`, matching how
     * [it.fast4x.rimusic.utils.downloadedStateMedia] already treats an unreachable cache.
     */
    fun isFullyCached( cache: Cache, key: String ): Boolean =
        runCatching {
            val length = ContentMetadata.getContentLength( cache.getContentMetadata( key ) )
            length > 0 && cache.isCached( key, 0, length )
        }.getOrDefault( false )
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.android.service.DownloadCacheStateTest"
```

Expected: PASS, 5 tests.

- [ ] **Step 5: Wire it into the badge**

In `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/utils/DownloadUtils.kt`, replace lines 28-40 (the function signature through the early return) with:

```kotlin
@UnstableApi
@Composable
fun downloadedStateMedia(
    mediaId: String,
    cache: Cache = koinInject(CacheType.CACHE),
    downloadCache: Cache = koinInject(CacheType.DOWNLOAD)
): DownloadedStateMedia {
    // A COMPLETED index row is not proof the bytes are still there — the index and the
    // download cache are independent stores. Require both, so a cache that was wiped or
    // evicted behind DownloadManager's back stops claiming the song is downloaded.
    val isDownloaded by remember( mediaId, downloadCache ) {
        MyDownloadHelper.getDownload( mediaId )
                        .map {
                            it?.state == Download.STATE_COMPLETED
                                    && DownloadCacheState.isFullyCached( downloadCache, mediaId )
                        }
    }.collectAsState( false, Dispatchers.IO )

    // Return early so it doesn't create another remember function
    if( isDownloaded )
        return DownloadedStateMedia.DOWNLOADED
```

Add this import alongside the existing ones at the top of the file:

```kotlin
import app.kreate.android.service.DownloadCacheState
```

Leave the rest of the function (the `try { cache.cacheSpace }` block and the `isCached` flow) exactly as it is.

Note the `remember( mediaId, downloadCache )` keys — the original `remember { }` had none, so the flow was never rebuilt when a recycled list slot changed `mediaId`.

- [ ] **Step 6: Verify the app still compiles**

```bash
./gradlew :composeApp:compileGithubUniversalProdDebugKotlin
```

Expected: BUILD SUCCESSFUL. `downloadedStateMedia` gained a *defaulted* parameter, so all six existing call sites (`SongItem.kt:187`, `SwipeableContent.kt:136` and `:222`, and the three via `isDownloadedSong`) keep compiling untouched.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "fix: downloaded badge now requires bytes in the download cache

The UI read media3's DownloadIndex while playback read the exo_downloads
SimpleCache. Nothing reconciled them, so a song whose bytes were removed
kept showing the downloaded badge forever while playback silently
re-resolved the stream from YouTube.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: Route every download deletion through DownloadManager

Task 1 stops the badge from lying. This task stops the desync from happening in the first place, so the bytes and the index row always move together.

`DownloadManager.removeDownload(id)` is a no-op when there is no index row for `id` — it logs "Failed to remove nonexistent download" and returns. So a single helper must branch: use `DownloadManager` when a row exists (it removes row *and* bytes), and fall back to a direct cache eviction only for orphaned bytes. Branching inside one helper also avoids racing the two removal paths against each other.

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/android/service/DownloadHelper.kt` (add two interface members)
- Modify: `composeApp/src/androidMain/kotlin/me/knighthat/impl/DownloadHelperImpl.kt` (implement them)
- Modify: `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/service/MyDownloadHelper.kt` (delegate them)
- Modify: `composeApp/src/androidMain/kotlin/me/knighthat/component/ResetCache.kt:38-63`
- Modify: `composeApp/src/androidMain/kotlin/me/knighthat/component/song/ResetSongDialog.kt:150-158`
- Modify: `composeApp/src/androidMain/kotlin/me/knighthat/component/tab/DeleteSongDialog.kt:36-37,52-66`
- Modify: `composeApp/src/androidMain/kotlin/me/knighthat/component/tab/HideSongDialog.kt:34-45`
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/component/menu/ResetSongButton.kt:123-133`
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/component/menu/DeleteSongButton.kt:41-52`
- Modify: `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/ui/components/themed/PlayerMenu.kt:74-84`
- Modify: `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/ui/components/themed/MediaItemMenu.kt:246-256`
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/component/settings/data/ExoCacheIndicator.kt`
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/screens/settings/DataSettings.kt:210-241`
- Create: `composeApp/src/androidUnitTest/kotlin/app/kreate/android/service/DownloadCacheOwnershipTest.kt`

**Interfaces:**
- Consumes: `DownloadCacheState.isFullyCached` from Task 1 (indirectly — nothing new).
- Produces:
  - `app.kreate.android.service.DownloadHelper.purgeDownload(songId: String)` — removes one song from the index *and* the download cache.
  - `app.kreate.android.service.DownloadHelper.purgeAllDownloads()` — removes every download from the index *and* the download cache.
  - `it.fast4x.rimusic.service.MyDownloadHelper.purgeDownload(songId: String)` and `.purgeAllDownloads()` — the object-style entry points the UI uses.

- [ ] **Step 1: Write the failing guard test**

This is an architecture test. It is the regression guard for the whole bug class: once it passes, no future edit can reintroduce a direct download-cache eviction without turning the build red.

Create `composeApp/src/androidUnitTest/kotlin/app/kreate/android/service/DownloadCacheOwnershipTest.kt`:

```kotlin
package app.kreate.android.service

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * media3's DownloadManager owns the download cache. Anything that deletes bytes from it
 * directly leaves the DownloadIndex claiming the song is still downloaded, which is exactly
 * the desync this guard exists to prevent.
 *
 * Only [me.knighthat.impl.DownloadHelperImpl] may touch the download cache directly, and only
 * to clean up bytes with no index row behind them.
 */
class DownloadCacheOwnershipTest {

    private companion object {
        /** Files permitted to combine a DOWNLOAD-cache reference with a direct eviction. */
        val ALLOWED = setOf( "DownloadHelperImpl.kt" )
    }

    private fun androidMainRoot(): File {
        var dir: File? = File( System.getProperty( "user.dir" )!! ).absoluteFile
        while ( dir != null ) {
            File( dir, "src/androidMain/kotlin" ).takeIf( File::isDirectory )?.let { return it }
            File( dir, "composeApp/src/androidMain/kotlin" ).takeIf( File::isDirectory )?.let { return it }
            dir = dir.parentFile
        }
        error( "Could not locate composeApp/src/androidMain/kotlin from ${System.getProperty( "user.dir" )}" )
    }

    @Test
    fun onlyDownloadHelperMayEvictFromTheDownloadCache() {
        val root = androidMainRoot()

        val offenders = root.walkTopDown()
                            .filter { it.isFile && it.extension == "kt" }
                            .filterNot { it.name in ALLOWED }
                            .filter { file ->
                                val text = file.readText()
                                "CacheType.DOWNLOAD" in text && "removeResource" in text
                            }
                            .map { it.relativeTo( root ).path }
                            .toList()

        assertTrue(
            offenders.isEmpty(),
            "These files evict from the download cache directly instead of going through " +
            "DownloadHelper.purgeDownload()/purgeAllDownloads(), which desyncs the " +
            "DownloadIndex from the cached bytes:\n" + offenders.joinToString( "\n" ) { "  - $it" }
        )
    }
}
```

- [ ] **Step 2: Run the guard to verify it fails**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.android.service.DownloadCacheOwnershipTest"
```

Expected: FAIL, listing exactly seven offenders:
```
  - me/knighthat/component/ResetCache.kt
  - me/knighthat/component/song/ResetSongDialog.kt
  - me/knighthat/component/tab/DeleteSongDialog.kt
  - app/kreate/android/themed/common/component/menu/DeleteSongButton.kt
  - app/kreate/android/themed/common/component/menu/ResetSongButton.kt
  - it/fast4x/rimusic/ui/components/themed/PlayerMenu.kt
  - it/fast4x/rimusic/ui/components/themed/MediaItemMenu.kt
```
(`HideSongDialog.kt` inherits `downloadCache` from `DeleteSongDialog` and does not itself name `CacheType.DOWNLOAD`, so it will not appear in this list — fix it anyway in Step 5. If the actual list differs from the seven above, reconcile before continuing: an extra entry is a call site this plan missed and must also be fixed in Step 5.)

- [ ] **Step 3: Add the two members to the `DownloadHelper` interface**

In `composeApp/src/androidMain/kotlin/app/kreate/android/service/DownloadHelper.kt`, add these two declarations immediately after `fun removeDownload( mediaItem: MediaItem )`:

```kotlin
    /**
     * Remove [songId] from **both** the download index and the download cache.
     *
     * Prefer this over evicting from the download cache directly: media3's `DownloadManager`
     * owns that cache, and deleting bytes behind its back leaves a `STATE_COMPLETED` index row
     * pointing at nothing.
     */
    fun purgeDownload( songId: String )

    /** [purgeDownload] for every download the index knows about. */
    fun purgeAllDownloads()
```

- [ ] **Step 4: Implement them in `DownloadHelperImpl`**

In `composeApp/src/androidMain/kotlin/me/knighthat/impl/DownloadHelperImpl.kt`, add this property next to the existing `executor`/`coroutineScope` declarations:

```kotlin
    private val downloadCache: Cache by inject( CacheType.DOWNLOAD )
```

and add these two functions immediately after the existing `override fun removeDownload( mediaItem: MediaItem )`:

```kotlin
    override fun purgeDownload( songId: String ) {
        coroutineScope.launch {
            if ( songId in downloads.value ) {
                // DownloadManager owns the bytes for anything it has an index row for;
                // it removes the row and the cached resource together.
                context.removeDownload<MyDownloadService>( songId ).exceptionOrNull()?.let {
                    if (it is CancellationException) throw it

                    Logger.e( it, "DownloadHelperImpl" ) { "purgeDownload failed for $songId!" }
                }
            } else {
                // No index row, so DownloadManager.removeDownload() would be a no-op and the
                // bytes would linger forever. Free them directly — nothing else can.
                runCatching { downloadCache.removeResource( songId ) }
                    .onFailure {
                        Logger.w( "DownloadHelperImpl" ) { "Could not evict orphaned bytes for $songId: ${it.message}" }
                    }
            }
        }
    }

    override fun purgeAllDownloads() {
        coroutineScope.launch {
            runCatching {
                DownloadService.sendRemoveAllDownloads(
                    /* context    = */ context,
                    /* clazz      = */ MyDownloadService::class.java,
                    /* foreground = */ false
                )
            }.onFailure {
                if (it is CancellationException) throw it

                Logger.e( it, "DownloadHelperImpl" ) { "purgeAllDownloads failed!" }
            }
        }
    }
```

Add these imports to the file:

```kotlin
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.DownloadService
import it.fast4x.rimusic.utils.removeDownload
import org.koin.core.component.inject
```

(`it.fast4x.rimusic.utils.removeDownload` and `org.koin.core.component.inject` may already be imported — check before adding, duplicates are a compile error.)

- [ ] **Step 5: Delegate from `MyDownloadHelper` and fix the eight call sites**

In `composeApp/src/androidMain/kotlin/it/fast4x/rimusic/service/MyDownloadHelper.kt`, add after `fun removeDownload(...)`:

```kotlin
    fun purgeDownload( songId: String ) = instance.purgeDownload( songId )

    fun purgeAllDownloads() = instance.purgeAllDownloads()
```

**`me/knighthat/component/ResetCache.kt`** — delete the `downloadCache` property (line 39) and replace the body of `onConfirm()`:

```kotlin
    override fun onConfirm() {
        Database.asyncTransaction {
            getSongs().forEach { song ->
                cache.removeResource( song.id )
                MyDownloadHelper.purgeDownload( song.id )
                formatTable.deleteBySongId( song.id )
                formatTable.updateContentLengthOf( song.id )
            }

            Toaster.done()
        }
    }
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`; remove `import app.kreate.di.CacheType` only if `cache` no longer needs it (it does — keep it).

**`me/knighthat/component/tab/DeleteSongDialog.kt`** — delete the `downloadCache` property (line 37) and replace the two lines in `onConfirm()`:

```kotlin
                cache.removeResource( it.id )
                MyDownloadHelper.purgeDownload( it.id )
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

**`me/knighthat/component/tab/HideSongDialog.kt`** — replace the same two lines in `onConfirm()`:

```kotlin
                cache.removeResource( it.id )
                MyDownloadHelper.purgeDownload( it.id )
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

**`me/knighthat/component/song/ResetSongDialog.kt`** — delete the `downloadCache` property and replace:

```kotlin
                    cache.removeResource( song.id )
                    MyDownloadHelper.purgeDownload( song.id )
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

**`app/kreate/android/themed/common/component/menu/ResetSongButton.kt`** — replace:

```kotlin
                    get<Cache>(CacheType.CACHE).removeResource( song.id )
                    MyDownloadHelper.purgeDownload( song.id )
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

**`app/kreate/android/themed/common/component/menu/DeleteSongButton.kt`** — replace the two lines in `onConfirm()`:

```kotlin
            get<Cache>(Cache::class.java, CacheType.CACHE).removeResource( song.mediaId )
            MyDownloadHelper.purgeDownload( song.mediaId )
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

**`it/fast4x/rimusic/ui/components/themed/PlayerMenu.kt`** — in the `isHiding` `ConfirmationDialog`'s `onConfirm`, delete the `downloadCache` declaration and replace the two eviction lines, leaving the block as:

```kotlin
                val cache: Cache by inject(Cache::class.java, CacheType.CACHE)

                cache.removeResource(mediaItem.mediaId)
                MyDownloadHelper.purgeDownload(mediaItem.mediaId)
                Database.asyncTransaction {
                    songTable.updateTotalPlayTime( mediaItem.mediaId, 0 )
                }
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

**`it/fast4x/rimusic/ui/components/themed/MediaItemMenu.kt`** — identical block, identical change:

```kotlin
                val cache: Cache by inject(Cache::class.java, CacheType.CACHE)

                cache.removeResource(mediaItem.mediaId)
                MyDownloadHelper.purgeDownload(mediaItem.mediaId)
                Database.asyncTransaction {
                    songTable.updateTotalPlayTime( mediaItem.mediaId, 0 )
                }
```
Add `import it.fast4x.rimusic.service.MyDownloadHelper`.

In every case, delete the now-stale `// FIXME: This is unsafe, use [DownloadService.sendRemoveDownload] instead` comment above the line you replaced.

- [ ] **Step 6: Fix the settings clear-all button**

`ExoCacheIndicator` is shared between the stream cache and the download cache, but only the stream cache may be cleared by direct eviction. Give it an injectable clear action.

Replace `composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/component/settings/data/ExoCacheIndicator.kt` in full:

```kotlin
package app.kreate.android.themed.common.component.settings.data

import androidx.compose.runtime.getValue
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import app.kreate.android.Preferences

@UnstableApi
class ExoCacheIndicator(
    private val preference: Preferences.Long,
    private val cache: Cache,
    /**
     * How to empty [cache]. Defaults to a direct eviction, which is correct for the stream
     * cache. The **download** cache must instead pass `MyDownloadHelper::purgeAllDownloads`,
     * because media3's `DownloadManager` owns it and a direct eviction would leave every
     * index row claiming the song is still downloaded.
     */
    private val clearAll: () -> Unit = { cache.keys.forEach( cache::removeResource ) }
): CacheUsageIndicator() {

    override fun updateProgress() {
        val maxSize by preference
        super.progress = if( maxSize == 0L )
            0f
        else
            cache.cacheSpace.toFloat() / maxSize
    }

    override fun onConfirm() {
        clearAll()
        updateProgress()
        hideDialog()
    }
}
```

Then in `composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/screens/settings/DataSettings.kt`, in the `entry( search, R.string.song_download_max_size )` block, change the indicator construction (currently at line 214) to:

```kotlin
                val indicator = remember( cache ) {
                    ExoCacheIndicator(
                        Preferences.EXO_DOWNLOAD_SIZE,
                        cache,
                        MyDownloadHelper::purgeAllDownloads
                    )
                }
```

Add `import it.fast4x.rimusic.service.MyDownloadHelper` to `DataSettings.kt`. Leave the `song_cache_max_size` entry's indicator (line 182) untouched — the default direct eviction is correct there.

- [ ] **Step 7: Run the guard to verify it passes**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.android.service.DownloadCacheOwnershipTest"
```

Expected: PASS.

- [ ] **Step 8: Run the whole unit test suite and compile**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest
./gradlew :composeApp:compileGithubUniversalProdDebugKotlin
```

Expected: BUILD SUCCESSFUL. (`SongPlaybackTest` hits the live network and skips itself when offline — a skip is not a failure.)

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "fix: route every download deletion through DownloadManager

Reset cache, reset song, hide song, delete song and the settings
clear-all button all evicted bytes from the download cache directly,
leaving a STATE_COMPLETED index row behind. They now go through
DownloadHelper.purgeDownload()/purgeAllDownloads(), and an
architecture test keeps it that way.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: Never evict from the download cache

`initCache` installs a `LeastRecentlyUsedCacheEvictor` whenever the configured max size is neither `0` nor `Long.MAX_VALUE`. On the download cache that is a silent data-loss bug: media3's `DownloadManager` assumes it is the only thing removing content, so downloading song N+1 quietly deletes song 1's bytes while song 1's index row stays `STATE_COMPLETED`. The download cache must always use `NoOpCacheEvictor`.

The size preference stays — the settings screen keeps reporting usage against it — it simply stops being enforced by eviction.

**Files:**
- Create: `composeApp/src/androidMain/kotlin/app/kreate/di/CacheEvictors.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/app/kreate/di/CacheEvictorsTest.kt`
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/di/PlayerModule.kt:47-78`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `app.kreate.di.cacheEvictorFor(maxSizeBytes: Long, allowEviction: Boolean): androidx.media3.datasource.cache.CacheEvictor`

The new function lives in its own file rather than in `PlayerModule.kt` on purpose: `PlayerModule.kt` has a top-level `val playerModule = module { ... }` whose initializer runs on class load, so a unit test touching anything in that file would drag Koin module construction into the test JVM.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/androidUnitTest/kotlin/app/kreate/di/CacheEvictorsTest.kt`:

```kotlin
@file:androidx.media3.common.util.UnstableApi

package app.kreate.di

import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import kotlin.test.Test
import kotlin.test.assertTrue

class CacheEvictorsTest {

    /**
     * The download cache is owned by media3's DownloadManager. Any evictor on it would
     * silently delete finished downloads while their index rows still say STATE_COMPLETED.
     */
    @Test
    fun downloadCacheNeverEvicts() {
        assertTrue( cacheEvictorFor( 2_048_000_000L, allowEviction = false ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( 32_000_000L, allowEviction = false ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( Long.MAX_VALUE, allowEviction = false ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( 0L, allowEviction = false ) is NoOpCacheEvictor )
    }

    @Test
    fun streamCacheEvictsWhenBounded() {
        assertTrue( cacheEvictorFor( 2_048_000_000L, allowEviction = true ) is LeastRecentlyUsedCacheEvictor )
    }

    @Test
    fun streamCacheDoesNotEvictWhenUnbounded() {
        assertTrue( cacheEvictorFor( Long.MAX_VALUE, allowEviction = true ) is NoOpCacheEvictor )
        assertTrue( cacheEvictorFor( 0L, allowEviction = true ) is NoOpCacheEvictor )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.di.CacheEvictorsTest"
```

Expected: compilation failure — `Unresolved reference: cacheEvictorFor`.

- [ ] **Step 3: Write the implementation**

Create `composeApp/src/androidMain/kotlin/app/kreate/di/CacheEvictors.kt`:

```kotlin
@file:androidx.media3.common.util.UnstableApi

package app.kreate.di

import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor

/**
 * Pick the evictor for a media cache.
 *
 * @param maxSizeBytes user-configured ceiling; `0` (disabled) and [Long.MAX_VALUE] (unlimited)
 *                     both mean "no LRU bound"
 * @param allowEviction `false` for the download cache. media3's
 *                      [androidx.media3.exoplayer.offline.DownloadManager] assumes it is the
 *                      only thing removing content from that cache — an evictor there deletes
 *                      finished downloads while their index rows still report
 *                      `STATE_COMPLETED`, so the UI keeps showing a downloaded badge for a
 *                      song that now has to be re-fetched from the network.
 */
fun cacheEvictorFor( maxSizeBytes: Long, allowEviction: Boolean ): CacheEvictor =
    if ( !allowEviction || maxSizeBytes == 0L || maxSizeBytes == Long.MAX_VALUE )
        NoOpCacheEvictor()
    else
        LeastRecentlyUsedCacheEvictor( maxSizeBytes )
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.di.CacheEvictorsTest"
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Use it from `PlayerModule`**

In `composeApp/src/androidMain/kotlin/app/kreate/di/PlayerModule.kt`, change the `initCache` signature and its first statement (lines 47-52) to:

```kotlin
private fun initCache(
    context: Context,
    size: Long,
    cacheDirName: String,
    allowEviction: Boolean
): Cache {
    val cacheEvictor = cacheEvictorFor( size, allowEviction )
```

Leave the `cacheDir` `when( size )` block below it untouched.

Then update the two call sites (lines 73-78):

```kotlin
    single( CacheType.CACHE ) {
        initCache( get(), Preferences.EXO_CACHE_SIZE.value, CACHE_DIRNAME, allowEviction = true )
    }
    single( CacheType.DOWNLOAD ) {
        initCache( get(), Preferences.EXO_DOWNLOAD_SIZE.value, DOWNLOAD_CACHE_DIRNAME, allowEviction = false )
    }
```

Finally remove the now-unused imports from `PlayerModule.kt`:

```kotlin
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
```

- [ ] **Step 6: Verify it compiles**

```bash
./gradlew :composeApp:compileGithubUniversalProdDebugKotlin
```

Expected: BUILD SUCCESSFUL, with no "unused import" warnings for the two removed imports.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "fix: never install an LRU evictor on the download cache

media3's DownloadManager assumes it owns the download cache. With a
finite EXO_DOWNLOAD_SIZE, LeastRecentlyUsedCacheEvictor silently
deleted finished downloads as new ones arrived while their index rows
still reported STATE_COMPLETED. The size setting stays as a usage
readout; it no longer drives eviction.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Stop decoding video during audio playback

When stream resolution falls through to `METHOD_ANDROID`, the format is a *progressive* itag 18/22 — muxed H.264 + AAC. ExoPlayer selects both tracks and spins up a hardware video decoder for a track nothing renders. This burns CPU and battery, pulls down 360p video the app throws away, and adds MediaCodec reclaim contention to playback.

**Files:**
- Create: `composeApp/src/androidMain/kotlin/app/kreate/android/service/player/AudioOnlyTrackSelection.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/app/kreate/android/service/player/AudioOnlyTrackSelectionTest.kt`
- Modify: `composeApp/src/androidMain/kotlin/app/kreate/di/PlayerModule.kt:146-155` (the `ExoPlayer.Builder` chain)

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `app.kreate.android.service.player.audioOnly(base: androidx.media3.common.TrackSelectionParameters): androidx.media3.common.TrackSelectionParameters`

- [ ] **Step 1: Write the failing test**

Robolectric is used here because `TrackSelectionParameters` reaches into `androidx.media3.common.util.Util`, which touches Android framework statics; a plain JVM test would need `returnDefaultValues`, which this module does not set.

Create `composeApp/src/androidUnitTest/kotlin/app/kreate/android/service/player/AudioOnlyTrackSelectionTest.kt`:

```kotlin
@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioOnlyTrackSelectionTest {

    /**
     * The METHOD_ANDROID fallback serves muxed H.264 + AAC (itag 18/22). Nothing in the app
     * renders ExoPlayer video, so the video track must never be selected.
     */
    @Test
    fun videoTrackIsDisabled() {
        val result = audioOnly( TrackSelectionParameters.Builder().build() )
        assertTrue( C.TRACK_TYPE_VIDEO in result.disabledTrackTypes )
    }

    @Test
    fun audioTrackStaysEnabled() {
        val result = audioOnly( TrackSelectionParameters.Builder().build() )
        assertFalse( C.TRACK_TYPE_AUDIO in result.disabledTrackTypes )
    }

    /** Must layer onto whatever parameters the player already carries, not replace them. */
    @Test
    fun preservesExistingParameters() {
        val base = TrackSelectionParameters.Builder()
                                           .setPreferredAudioLanguage( "de" )
                                           .build()
        val result = audioOnly( base )
        assertTrue( "de" in result.preferredAudioLanguages )
        assertTrue( C.TRACK_TYPE_VIDEO in result.disabledTrackTypes )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.android.service.player.AudioOnlyTrackSelectionTest"
```

Expected: compilation failure — `Unresolved reference: audioOnly`.

- [ ] **Step 3: Write the implementation**

Create `composeApp/src/androidMain/kotlin/app/kreate/android/service/player/AudioOnlyTrackSelection.kt`:

```kotlin
@file:androidx.media3.common.util.UnstableApi

package app.kreate.android.service.player

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters

/**
 * Disable video track selection.
 *
 * The `METHOD_ANDROID` last-resort fallback in
 * [app.kreate.di.resolveInnertubeMedia] returns a *progressive* format (itag 18/22), which is
 * muxed H.264 + AAC. Without this, ExoPlayer selects the video track and starts a hardware
 * video decoder for frames nothing ever draws.
 *
 * Kreate renders no ExoPlayer video anywhere — the one video surface in the app
 * (`it.fast4x.rimusic.ui.screens.player.components.YoutubePlayer`) is a third-party embedded
 * IFrame player with its own pipeline, unaffected by this.
 */
fun audioOnly( base: TrackSelectionParameters ): TrackSelectionParameters =
    base.buildUpon()
        .setTrackTypeDisabled( C.TRACK_TYPE_VIDEO, true )
        .build()
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "app.kreate.android.service.player.AudioOnlyTrackSelectionTest"
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Apply it to the player**

In `composeApp/src/androidMain/kotlin/app/kreate/di/PlayerModule.kt`, in the `single<StatefulPlayer>` block, change the `ExoPlayer.Builder(...)` chain so the built player gets the parameters. Replace:

```kotlin
            ExoPlayer.Builder(get<Context>())
                .setMediaSourceFactory( dataSource )
                .setHandleAudioBecomingNoisy( true )
                .setWakeMode( C.WAKE_MODE_NETWORK )
                .setAudioAttributes( audioAttributes, handleAudioFocus )
                .setUsePlatformDiagnostics( false )
                .build()
```

with:

```kotlin
            ExoPlayer.Builder(get<Context>())
                .setMediaSourceFactory( dataSource )
                .setHandleAudioBecomingNoisy( true )
                .setWakeMode( C.WAKE_MODE_NETWORK )
                .setAudioAttributes( audioAttributes, handleAudioFocus )
                .setUsePlatformDiagnostics( false )
                .build()
                .apply {
                    // The ANDROID progressive fallback is muxed H.264 + AAC; without this
                    // ExoPlayer spins up a video decoder for frames nothing renders.
                    trackSelectionParameters = audioOnly( trackSelectionParameters )
                }
```

Add this import to `PlayerModule.kt`:

```kotlin
import app.kreate.android.service.player.audioOnly
```

- [ ] **Step 6: Verify it compiles and the suite passes**

```bash
./gradlew :composeApp:compileGithubUniversalProdDebugKotlin
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "perf: disable video track selection during playback

The METHOD_ANDROID stream fallback serves muxed H.264 + AAC (itag
18/22), so ExoPlayer was starting a hardware video decoder for frames
nothing renders. Field logs show c2.exynos.h264.decoder running during
ordinary audio playback.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: Verify on device

The three download tasks change behaviour that no unit test can observe end to end. Verify against a real build before calling this done.

**Files:** none — verification only.

- [ ] **Step 1: Build the release variant**

Per project convention, verification happens on the release variant (the debug build is too slow to judge playback on a phone). It builds unsigned without `OFFICIAL_BUILD_PASSPHRASE`, which is fine here.

```bash
./gradlew :composeApp:assembleGithubUniversalProdRelease
```

Expected: BUILD SUCCESSFUL. The APK lands at `composeApp/build/outputs/apk/githubUniversalProd/release/`.

- [ ] **Step 2: Run Android Lint**

```bash
./gradlew :composeApp:lintGithubUniversalProdDebug
```

Expected: no new errors introduced by these changes.

- [ ] **Step 3: Confirm the badge now tells the truth**

Install the build and check, in order:

1. Open a playlist that currently shows songs as downloaded which actually stream. **Expected:** the badges are now gone — Task 1 made the check self-healing, no migration needed.
2. Download one song. **Expected:** badge appears.
3. Play it with the device in airplane mode. **Expected:** it plays; `adb logcat | grep dataspec` shows **no** `Resolved <id> via ...` line for it.
4. Long-press the song → Delete / Hide. **Expected:** badge gone, and Settings → Data → "Song download max size" usage drops by roughly the song's size.
5. Settings → Data → "Song download max size" → clear-all. **Expected:** usage drops to zero **and** every download badge in the app disappears (this is the `purgeAllDownloads` path from Task 2 — before this change the badges would have survived).

- [ ] **Step 4: Confirm no video decoder starts**

```bash
adb logcat -c && adb logcat | grep -iE "h264|hevc|CCodec|dataspec"
```

Play a song that falls back to the ANDROID progressive client (the log line `Resolved <id> via ANDROID` identifies one). **Expected:** the `Resolved ... via ANDROID` line still appears, but no `c2.*.h264.decoder` / `CCodecBufferChannel` lines follow it, and playback is unaffected.

- [ ] **Step 5: Squash the harness `wip:` commits**

```bash
git log --oneline -20
```

Fold any remaining `wip: <path>` commits into the four feature commits before pushing.

---

## Self-review notes

- **Spec coverage:** Background item 1 (the two-store split) → Task 1. Items 1-6 (the six direct-eviction sites) → Task 2. Item 7 (LRU evictor) → Task 3. The video-decode finding → Task 4. All covered.
- **Deliberately out of scope:** the main-thread `Palette.generate()` jank in `MainActivity.kt:384` (the other half of the "playback lag" report), the PO-token WebView failure on GrapheneOS/Vanadium, and the six blocking HEAD probes in `validateStreamUrl`. Each is a separate, independently testable change.
- **Type consistency check:** `purgeDownload(songId: String)` and `purgeAllDownloads()` are declared identically in `DownloadHelper`, `DownloadHelperImpl`, `MyDownloadHelper`, and every call site. `cacheEvictorFor(maxSizeBytes, allowEviction)` matches between `CacheEvictors.kt`, its test, and both `initCache` call sites. `audioOnly(base)` matches between its file, its test, and `PlayerModule`. `DownloadCacheState.isFullyCached(cache, key)` matches between its file, its test, and `DownloadUtils.kt`.

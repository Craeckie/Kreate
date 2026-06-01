# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

Kreate is an Android (Kotlin Multiplatform / Compose Multiplatform) YouTube Music client, originally a fork of RiMusic, itself a fork of ViMusic. This repository is a further fork from `knighthat/Kreate` whose primary goal is fixing playback crashes/stops. Because the lineage spans multiple maintainers, expect inconsistent coding styles, multiple package roots covering similar concerns, and dead-ends from older code paths.

The Android app is the production target. The JVM/desktop module exists but is largely a stub (`composeApp/src/desktopMain/kotlin/main.kt` just opens a window with a placeholder UI) — do not assume desktop builds work and don't break Android trying to fix it.

## Build, run, test

This project uses Gradle with Kotlin Multiplatform + the Android Application plugin. The Android module is `:composeApp`. Java/Kotlin toolchain is **JVM 21**.

The build matrix is **3 platform flavors × 5 arch flavors × 2 env flavors × 4 build types**. The default debuggable variant is `githubUniversalProdDebug`.

```bash
# Most common dev build (debug, github platform, universal arch, prod env)
./gradlew :composeApp:assembleGithubUniversalProdDebug

# Install onto a connected device/emulator
./gradlew :composeApp:installGithubUniversalProdDebug

# Run unit tests (JUnit4 + Robolectric for Android, kotlin.test for common)
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest    # Android tests
./gradlew :composeApp:jvmTest                                  # commonMain/jvmMain tests

# The Android unit tests include a live-network playback test
# (app.kreate.android.service.innertube.SongPlaybackTest) that resolves a real
# YouTube stream via the ANDROID_VR client and asserts it serves byte ranges
# past the 1-minute mark — guarding the 403 / "stops at ~1 min" regression. It
# honours the env proxy and SKIPS itself (JUnit assumption) when offline, so it
# never breaks a no-network build; a hard failure means songs genuinely won't play.

# Run a single test class
./gradlew :composeApp:testGithubUniversalProdDebugUnitTest \
  --tests "it.fast4x.rimusic.utils.AppLifecycleTrackerTest"

# Android Lint (configured via `android.lint` plugin)
./gradlew :composeApp:lintGithubUniversalProdDebug

# Release builds (requires signing env: STORE_PASSWORD, KEY_PASSWORD,
# OFFICIAL_BUILD_PASSPHRASE — release builds are unsigned otherwise)
./gradlew :composeApp:assembleGithubUniversalProdRelease
./gradlew :composeApp:assembleGithubUniversalProdUncompressed   # release w/o R8 minify
```

The release notes copy task (`copyReleaseNote`) runs automatically before any non-debug build; it reads `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.

### Creating a release (Debian-style backport versioning)

This fork uses a Debian-style backport version scheme: `<upstream>-<N>` where N starts at 1 and increments if a second release is needed without an upstream bump.

**Version encoding in `gradle/libs.versions.toml`:**
- `versionName` = `"<upstream_version>-<N>"`, e.g. `"2.2.0-1"`, `"2.2.0-2"`, `"2.2.1-1"`
- `versionCode` = `upstream_versionCode * 100 + N`, e.g. upstream `138` → `13801`, `13802`; upstream `139` → `13901`

This keeps Android's required monotonic ordering: `13801 < 13802 < 13901`, and is always higher than upstream's raw code.

**Steps for a new release:**

1. Determine the upstream base:
   ```
   upstream versionCode = N  (from gradle/libs.versions.toml before rebase, or from knighthat/Kreate)
   upstream versionName = X.Y.Z
   ```
2. Set our version in `gradle/libs.versions.toml`:
   - Same upstream version as last release → increment N: `versionCode = upstreamCode * 100 + N`, `versionName = "X.Y.Z-N"`
   - New upstream version → reset N to 1: `versionCode = newUpstreamCode * 100 + 1`, `versionName = "newX.Y.Z-1"`
3. Create `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` with user-facing notes.
4. Commit both files and push to `main`.
5. Manually trigger the **Build all flavors** workflow (`workflow_dispatch`) on GitHub Actions.

CI will fail if the changelog file is missing or `versionName` matches the latest release tag on this fork.

Submodules `modules/innertube` and `modules/kizzy` must be checked out (`git submodule update --init --recursive`) — they're separate Gradle projects included via `settings.gradle.kts`.

## Architecture overview

### Module layout (`settings.gradle.kts`)
- `:composeApp` — main app, Android + JVM source sets
- `:innertube`, `:kizzy`, `:kizzyDomain` — git submodules under `modules/`
- `:oldtube`, `:kugou`, `:lrclib`, `:discord` — local in-repo libraries under `extensions/`

There are two `innertube` modules used in parallel:
- `extensions/innertube` (project `:oldtube`) is the older RiMusic-era YouTube client (`it.fast4x.innertube`)
- `modules/innertube` (project `:innertube`, submodule) is the newer client (`me.knighthat.innertube`)

Both are still referenced from app code; new work should generally target the newer one and bridge through `app.kreate.android.service.innertube.InnertubeProvider`.

### Source set layout
- `composeApp/src/commonMain/kotlin/app/kreate/...` — KMP-shared code: Room DB schema and entities (`app.kreate.database`), DI module declarations (`app.kreate.di`), constants, utilities. **The Room database schema lives here**, not in `androidMain`.
- `composeApp/src/androidMain/kotlin/...` — Android-specific code, the bulk of the app. Three coexisting top-level package roots reflect the fork lineage:
  - `it.fast4x.rimusic.*` — original RiMusic code (`MainActivity`, `MainApplication`, the `Database` accessor singleton, lots of UI screens/components).
  - `me.knighthat.*` — knighthat-era additions (components, downloader impl, sync, updater, utilities).
  - `app.kreate.*` — newest layer, where active rewrites land (playback service, player abstractions, DI modules, viewmodels, widgets, screens).
  When fixing things, prefer extending or moving code into `app.kreate.*` rather than scattering across all three.
- `composeApp/src/{jvmMain,desktopMain}` — desktop stub; expect-actual `.jvm.kt` files for DI/network/database.
- `composeApp/src/{androidGithub,androidFdroid,androidIzzy,androidDebug}` — flavor/build-type source sets (e.g. update-checker behavior differs by platform flavor).

### Playback pipeline (most relevant for fixing playback crashes)

The runtime player is wired together via Koin DI. Trace any playback bug through these layers:

1. **`MainApplication` (`it.fast4x.rimusic.MainApplication`)** — installs `CrashHandler`, initializes Koin, registers the new-style `Innertube` provider, registers a global `ConnectivityManager` callback, and configures a single Coil `ImageLoader`.

2. **`PlaybackService` (`app.kreate.android.service.playback.PlaybackService`)** — `MediaLibraryService` subclass declared in `AndroidManifest.xml` with `foregroundServiceType="mediaPlayback"`. Owns the `MediaLibrarySession`, the injected `StatefulPlayer`, the `DownloadHelper`, and a `VolumeObserver`. It also reacts to `SharedPreferences` changes and registers a `LiveWallpaperEngine` when enabled.

3. **`StatefulPlayer` / `StatefulPlayerImpl` (`app.kreate.android.service.player`)** — wraps an ExoPlayer with shuffle/repeat/sleep-timer/radio state. The injected singleton is constructed in `app.kreate.di.PlayerModule`.

4. **`PlayerModule` (`app.kreate.di/PlayerModule.kt`)** — the most load-bearing file for stream resolution. It wires up:
   - A two-tier cache `ResolvingDataSource.Factory`: `downloadCache` over `cache` over `DefaultDataSource` with `OkHttpDataSource` upstream. Both caches set `FLAG_IGNORE_CACHE_ON_ERROR`.
   - The `resolver(...)` lambda that, for non-local URIs, calls `upsertSongInfo` (writes song metadata to Room), then `getPlayableUrl(songId)` which calls `makeStreamCache(...)`. `makeStreamCache` walks the InnerTube client fallback chain (`nextFallbackMethod`): **`METHOD_ANDROID_VR → METHOD_ANDROID (reel) → METHOD_WEB → METHOD_IOS`** (IOS terminal). The chosen URL is validated with a `HEAD` range request, then cached in an in-memory `ConcurrentMap<String, StreamCache>` keyed by song id, with expiry tracking. See the **YouTube stream resolution** subsection below — the VR-first chain is the fix for the 403 / "stops at ~1 min" failures.
   - `extractFormat` picks an audio adaptive format by `AudioQualityFormat` preference, `extractStreamUrl` deobfuscates `signatureCipher` via `YoutubeJavaScriptPlayerManager` (a no-op for VR/mobile URLs, which are pre-signed and carry no `n` param).
   - `clearCachedStreamUrlOf(songId)` is the escape hatch when a cached URL goes stale — call it before retrying a failed playback.
   - Diagnostic breadcrumbs are logged at **`Info`** (`logger.i`, `dataspec` tag) per client attempt: chosen client, `playabilityStatus`, itag, HEAD result, and `summarizeStreamUrl` (the `c=`/`itag=`/`expire=`/`pot=` of the resolved URL). Info level is deliberate — release builds default `minSeverity` to `Info`, so `.d`/`.v` lines are invisible in the field (see Logging note).

5. **`PlaybackExceptions` (`it.fast4x.rimusic.service.PlaybackExceptions`)** — domain-specific `PlaybackException` subclasses (`PlayableFormatNotFoundException`, `UnplayableException`, `LoginRequiredException`, `MissingDecipherKeyException`, `NoInternetException`, `TimeoutException`, …). Use these rather than generic exceptions when raising errors from the player layer; they carry `ERROR_CODE_*` ints that the UI layer keys off.

6. **`ErrorHandlingPolicy` (`app.kreate.android.service.player.ErrorHandlingPolicy`)** — returns `false` (not eligible for ExoPlayer's internal retry) for `HttpDataSource.InvalidResponseCodeException`, so a 403 surfaces to `onPlayerError` immediately instead of being silently replayed against the same stale URL 5× first. `StatefulPlayerImpl.onPlayerError` then handles a 403 by clearing the cached URL and re-resolving once (re-resolution restarts at the default `METHOD_ANDROID_VR`), guarded by `retried403Songs` and reset on `STATE_READY`.

### YouTube stream resolution: 403 / PO-token / client selection

This is the #1 source of "won't play" reports. Findings (validated 2026-05-30 against yt-dlp 2026.03 and NewPipe, and reproduced with `scripts/vr_probe.py`):

- **Root cause of 403s:** as of 2026 YouTube requires a **GVS (streaming) PO token** for HTTPS playback on the `ANDROID`, `IOS`, `WEB`, and `WEB_EMBEDDED` clients. Kreate does not reliably attach one (the WebView PO token often returns `null`), so those clients fail.
- **Two surface symptoms, one cause.** Without a pot, YouTube either 403s the URL outright, **or** serves a ~1-minute *teaser* then 403s every later byte range — which presents as **"song starts but stops around the 1-minute mark."** Both are the missing-pot root cause. (`scripts/vr_probe.py` reproduces the teaser-block: on a failing video, IOS returns `206` for ranges at 0s/30s but `403` at 60s/90s/tail.)
- **`validateStreamUrl` is a weak signal.** Its `HEAD` only probes range `0–512KB` (the teaser), so it passes even for a URL that dies at 60s. Do not treat HEAD-OK as "this URL will play to the end."
- **The fix — `ANDROID_VR`.** yt-dlp's default JS-less client `android_vr` (clientVersion `1.65.10`) needs **no PO token and no JS player** (no signature cipher). It is tried first (`METHOD_ANDROID_VR`) and streams fully. Built in `app.kreate.android.service.innertube.AndroidVrStreamHelper` — NewPipe v0.26.0 ships no VR helper, so it mutates `InnertubeClientRequestInfo.ofAndroidClient()` into the VR client using NewPipe's public helpers (`YoutubeParsingHelper.prepareJsonBuilder` / `getVisitorDataFromInnertube` / `getValidJsonResponseBody`, `NewPipe.getDownloader()`).
  - VR caveats (why the legacy chain is kept as fallback): "made for kids" videos return `UNPLAYABLE`, and clientVersion > 1.65 may return SABR-only streams (so the version is pinned).
- **Not `n`-throttling.** Mobile-client (VR/IOS) URLs carry no `n` throttling param, so the classic throttle-stall is not the mechanism here; `getUrlWithThrottlingParameterDeobfuscated` is a no-op for them.

**When investigating a "won't play" / "stops partway" report:**
- Reference extractors are checked out in-repo: `yt-dlp/yt_dlp/extractor/youtube/` (esp. `_base.py` `INNERTUBE_CLIENTS` and the PO-token policies) and `NewPipeExtractor/extractor/`. Use them to learn which clients currently work — YouTube changes this often.
- `scripts/vr_probe.py <videoId>` replicates the VR vs IOS player request and probes byte ranges with real HTTP `Range` headers — it distinguishes outright-403, ~1-min teaser-block, and throttling without an Android build.
- Ask the user to raise `RUNTIME_LOG_SEVERITY` (or use a debug build) and capture the `dataspec` Info logs; they show which client won and whether a `pot=` was attached.

### Database

Room KMP database, defined in `app.kreate.database.AppDatabase` (commonMain) at version **36**, with a long chain of `AutoMigration`s plus several custom `*Migration` specs in `app/kreate/database/migration/`. Schemas are exported to `composeApp/schemas/` — bumping `version` requires committing the new schema JSON.

Access goes through the `it.fast4x.rimusic.Database` Kotlin object (Android only), which is a Koin-injected singleton wrapper exposing each DAO and providing two execution helpers:

- `Database.asyncTransaction { ... }` — write path. Runs on `transactionExecutor` (Room-managed), preserves atomicity. **Use this for all writes**, including bulk operations.
- `Database.asyncQuery { ... }` — read path. Runs on `queryExecutor`. **Do not write from this.**

Recent commit `b0edfef4b "fix: multiple conflicting SQLite connections"` refactored cache and download SQLite usage — be wary of opening additional `StandaloneDatabaseProvider` instances for the media3 cache / download tables; reuse the DI-provided singleton.

The DB filename is profile-aware: `data.db` for the default profile, `data_<profile>.db` otherwise (`Database.FILE_NAME`).

### DI (Koin)

Modules are split per concern with `expect-actual` for platform specifics:
- `commonMain/app/kreate/di/`: `DatabaseModule`, `NetworkModule`, `ImageModule`, `ViewModelModules`, `ExternalServicesModule`, `initKoin`
- `androidMain/app/kreate/di/`: same names with `.android.kt` actuals + Android-only `PlayerModule`, `CacheModule`, `PreferencesModule`
- `jvmMain/app/kreate/di/`: `.jvm.kt` actuals (mostly stubs)

Koin is started in `MainApplication.onCreate` with a `KoinBufferedLogger` so that early log lines aren't lost before `setupLogging` configures Kermit.

### Other things worth knowing

- **Crash log handling**: `CrashHandler` writes uncaught exceptions to `<externalFilesDir>/crashlogs/Kreate_crashlog_<datetime>.log` and calls `exitProcess(1)`. The `CopyCrashlogActivity` is exposed via the `COPY_CRASH_LOG` action so crash dialogs can copy logs to clipboard. When investigating playback crashes, ask the user for these files.
- **`Preferences` (`app.kreate.android.Preferences`)**: a single object exposing typed `SharedPreferences`-backed `MutableState`s. Read/write through this object — don't construct your own `SharedPreferences` consumers.
- **Logging**: Kermit (`co.touchlab.kermit.Logger`) is the standard. The `dataspec` tag is used heavily in the playback resolver path. **Release builds default `minSeverity` to `Severity.Info` (`setupLogging` / `Preferences.RUNTIME_LOG_SEVERITY`), so `.d`/`.v` lines never reach field logs** — log anything you need in user-submitted logs at `Info` (`logger.i`) or higher, or have the user raise `RUNTIME_LOG_SEVERITY`. Debug builds force `Verbose`.
- **Network security**: `usesCleartextTraffic="false"` plus `@xml/network_security_config` — adding new HTTP endpoints means updating the network security config.
- **Build phrase gate**: `OFFICIAL_BUILD_PASSPHRASE` env var, hashed and checked against a hard-coded SHA-256 in `composeApp/build.gradle.kts`, gates the production signing config. Forks (including this one) without that env will produce unsigned release APKs. Don't change the hash check.
- **APK naming**: the `applicationVariants.all { ... outputFileName = ... }` block in `composeApp/build.gradle.kts` renames outputs to `Kreate Fixed-<arch-or-buildtype>.apk`. The variable `APP_NAME` near the top of that file is the source of truth.
- **`applicationIdSuffix = ".fix"`** is set at `defaultConfig` level in this fork, so this app installs side-by-side with upstream Kreate.

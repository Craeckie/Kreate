# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Subagents

You have standing permission to spawn Explore, Plan, and general-purpose subagents whenever it would help — e.g., broad fan-out searches across the codebase, parallel investigation of multiple files or modules, or validating an approach before implementing. You do not need to wait for the user to ask.

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
# (app.kreate.android.service.innertube.SongPlaybackTest) that walks the real
# resolver chain — ANDROID_VR -> IOS -> ANDROID progressive — and asserts that
# *some* client serves byte ranges past the 1-minute mark, guarding the 403 /
# "stops at ~1 min" regression. It passes as soon as one rung streams the whole
# track, because that is when a song plays; pinning it to ANDROID_VR alone would
# fail over a YouTube change the app already absorbs. It honours the env proxy and
# SKIPS itself (JUnit assumption) when offline, so it never breaks a no-network
# build; a hard failure means the whole chain is exhausted and songs genuinely
# won't play.

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
   **Changelog format** — use Markdown with emoji sections (same style as `../CardPop`):
   - Group entries under `##` headings that each open with a section emoji, e.g.:
     `## ✨ New Features`, `## 🐛 Fixes`, `## 🎨 Visual`, `## 🔧 Changes`, `## 🗑️ Removed`
   - Each bullet starts with a relevant emoji and uses **bold** for key feature/area names, e.g.:
     `- 🎵 **Auto-rematch** — songs whose YouTube video became unavailable are re-linked automatically`
   - Write for end users, not developers: skip internal class names, commit hashes, PR numbers.
   - End the file with a full-changelog footer (update the tag range):
     `**Full Changelog**: https://github.com/Craeckie/Kreate/compare/v<prev>...v<this>`
   - Omit sections that have no entries for this release.

   Example:
   ```
   ## ✨ New Features
   - 🔄 **Download rematch** — re-download a song after it has been re-linked to a new video

   ## 🐛 Fixes
   - 💥 Fix crash when two rematch calls raced on the same song
   - 🔕 Fix ForegroundServiceStartNotAllowedException on some devices

   **Full Changelog**: https://github.com/Craeckie/Kreate/compare/v2.2.0-6...v2.2.0-7
   ```

4. Commit both files and push to `main`.
5. Create and push the release tag — this automatically triggers the **Build all flavors** workflow:
   ```bash
   git tag v<versionName>   # e.g. git tag v2.2.0-5
   git push origin v<versionName>
   ```

CI will fail if the changelog file is missing or `versionName` matches the latest release tag on this fork.

### CI workflows that are red for non-code reasons

Two workflows fail on this fork independently of what we push — verified 2026-09-04. Don't
chase them when a push turns the Actions tab red:

- **Chores → "Update license report"** — `./gradlew generateLicenseReport` dies on a Gradle
  variant-ambiguity error resolving `:discord` (an AGP KMP library) for
  `githubUniversalProdUncompressedRuntimeClasspath`. The `licenseReport { }` block and
  `extensions/discord/build.gradle.kts` are identical to upstream, and *every* push-triggered
  Chores run on `knighthat/Kreate` fails the same way. The job only regenerates
  `res/raw/licenses.json`. It triggers on any push touching `**/build.gradle.kts` or
  `gradle/libs.versions.toml`, so an upstream merge always fires it.
- **Build all flavors (bi-monthly schedule)** — stops at "Verify versions" with `Matched
  upstream and downstream versions`. That is the intended guard: nothing new to release. The
  tag-triggered runs, which are the ones that matter, succeed.

**Build (Nightly)** used to be a third: it ran daily and died in ~10s on
`jq: syntax error … select(.workflow_id == )`, because upstream's `build-nightly.yaml` reads
a `${{ vars.WORKFLOW_ID }}` repository variable that this fork never set. The daily
`schedule:` trigger is now removed and the step derives the workflow file name from
`github.workflow_ref` instead, so nothing needs configuring. It is dispatch-only. Note that
re-enabling it needs more than the schedule back: the build job still expects upstream's
`NIGHTLY_KEYSTORE`, `NIGHTLY_KEYSTORE_SHA256`, `NIGHTLY_KEY_PASSWORD`, `STORE_PASSWORD` and
`RELEASE_TOKEN` secrets plus a `KEYSTORE_PATH` variable, none of which exist here — this
fork signs with `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`.

Both `.github/workflows/build-nightly.yaml` and `.github/workflows/build-all-flavors-weekly.yml`
are in the `PROTECTED` list in `scripts/upstream_merge.py`, so an upstream merge cannot hand
them back to upstream's versions.

### Syncing with upstream (knighthat/Kreate)

**Merge, don't cherry-pick.** Full rationale and the trap list live in
[`docs/upstream-merge.md`](./docs/upstream-merge.md) — read it before syncing.

```bash
python3 scripts/upstream_merge.py            # fetch + dry-run report
python3 scripts/upstream_merge.py --merge    # merge, auto-resolving PROTECTED paths to ours
python3 scripts/upstream_merge.py --replay HEAD   # re-analyse an existing merge
```

The script buckets everything upstream touched into `CONFLICT` (git stopped),
`PROTECTED` (our version always wins — auto-resolved), and **`SILENT`** (both
sides changed the file and git merged it clean). `SILENT` is the dangerous one:
a clean `git merge` of the 2026-07 batch silently introduced swapped PO tokens,
a Compose duplicate-key crash, and a `DELETE` that wipes every local file's
playlist rows. **Read `git diff HEAD -- <path>` for every SILENT and
ALWAYS REVIEW path before committing**, then verify with a build.

The `PROTECTED` / `ALWAYS_REVIEW` lists at the top of `scripts/upstream_merge.py`
are the policy; each entry carries its reason. Edit them when a subsystem stops
being dead or a new one diverges.

`docs/upstream-triage.tsv` is a notebook, not a gate — a merge does not honour a
cherry-pick verdict, so anything marked `skipped`/`deferred` arrives anyway
unless you resolve it back out. The script re-prints those notes for the commits
a merge would bring in. `scripts/upstream_check.py` still gives the per-commit
cherry-pick view if you need it.

Three standing constraints:

- **Submodule pointers cannot be bumped from this machine.** `modules/innertube`
  and `modules/metrolist` point at `git@github.com:Craeckie/…` forks and there is
  no SSH key here; our `Kreate-innertube` fork is a squashed orphan sharing no
  history with upstream's. Always resolve them to ours.
- **`origin` is HTTPS and `git push` works** — `gh auth setup-git` installed
  `gh auth git-credential` as the credential helper, so plain `git push origin main` (and
  `--tags`) authenticates as `Craeckie`. The submodule remotes are still SSH, hence the
  point above.
- **Much of upstream's playback investment lands on code we do not execute.**
  `com.metrolist.music.utils.YTPlayerUtils` and the whole `utils/cipher/` package
  have **zero callers** in this fork; our resolver is
  `InnertubeResolvingDataSource` → `AndroidVrStreamHelper`. Taking upstream's
  rewrite of them buys nothing at runtime and drags in ~400KB of unused assets.
  Check for a live caller before taking a "fix" there.

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

4. **`PlayerModule` (`app.kreate.di/PlayerModule.kt`)** — wires up the ExoPlayer data source chain: `downloadCache` over `cache` over `DefaultDataSource` with `OkHttpDataSource` upstream. The `resolver(...)` lambda calls `resolveInnertubeMedia` from `InnertubeResolvingDataSource.kt`.

5. **`InnertubeResolvingDataSource.kt` (`app.kreate.di`)** — the current stream resolution entry point. For non-local URIs it calls `upsertSongInfo` (Room write), then `getPlayableUrl(songId)`. `getPlayableUrl` caches the resolved `YTPlayerUtils.PlaybackData` in a `ConcurrentHashMap` keyed by song id. Cache misses delegate to `YTPlayerUtils.playerResponseForPlayback(...)`.

6. **`YTPlayerUtils` (`com.metrolist.music.utils.YTPlayerUtils`)** — Metrolist-derived multi-client resolver. Uses `WEB_REMIX` as main client, with a long `STREAM_FALLBACK_CLIENTS` array tried in order: `TVHTML5_SIMPLY_EMBEDDED_PLAYER, TVHTML5, ANDROID_VR_1_43_32, ANDROID_VR_1_61_48, ANDROID_CREATOR, IPADOS, ANDROID_VR_NO_AUTH, MOBILE, IOS, WEB, WEB_CREATOR`. Key invariant: **`getOrNull()` is used on the main client**, so an HTTP 400 from YouTube does not abort the function — the ANDROID_VR clients in the fallback array are still tried.
   - `CipherDeobfuscator` (`com.metrolist.music.utils.cipher`) — singleton that holds a `WebView` for JS-based signature deobfuscation and n-param transform. **Must be initialized via `CipherDeobfuscator.initialize(context)` in `MainApplication.onCreate()`** before any playback; failing to do so causes `UninitializedPropertyAccessException` in `PlayerJsFetcher` and `PoTokenGenerator`.
   - `clearCachedStreamUrlOf(songId)` in `InnertubeResolvingDataSource.kt` is the escape hatch when a cached URL goes stale.

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

1. **Get the logs.** Ask the user to share a logcat file (bug reporter in-app, or `adb logcat`). Release builds log at `Info` minimum; the relevant tag is `YTPlayerUtils`.

2. **Run the analysis script first:**
   ```bash
   python3 scripts/logcat_analyze.py <logcat.txt>
   # or for a specific video
   python3 scripts/logcat_analyze.py <logcat.txt> --video <videoId>
   # or live
   adb logcat | python3 scripts/logcat_analyze.py -
   ```
   The script flags: `CipherDeobfuscator` not initialized, HTTP 400/403 at the player endpoint, PO-token failures, signature timestamp failures, which client won or whether all failed.

3. **Interpret the summary:**
   - `[CRITICAL] CipherDeobfuscator.appContext not initialized` → `CipherDeobfuscator.initialize(this)` is missing from `MainApplication.onCreate()`. This causes `PlayerJsFetcher` and `PoTokenGenerator` to crash for *every* song.
   - `[WARN] music.youtube.com/player returned 400` → YouTube is blocking the IP or fingerprinting `WEB_REMIX`. The fallback chain (ANDROID_VR etc.) should activate — if it doesn't, check that the main client uses `getOrNull()` not `getOrThrow()` at `YTPlayerUtils.kt` line ~123.
   - `[WARN] PO-token generation failed` + no `[OK] Successful playback resolutions` → all web clients fail. ANDROID_VR clients should not need a PO token.
   - `[ERROR] All clients exhausted` → check `STREAM_FALLBACK_CLIENTS` in `YTPlayerUtils.kt` vs yt-dlp `INNERTUBE_CLIENTS` to see what changed.
   - `[WARN] 403 on stream URL` → ANDROID_VR URL expiry or a VR client version YouTube no longer accepts. Check `ANDROID_VR_1_43_32` / `ANDROID_VR_1_61_48` clientVersions against yt-dlp.

4. **Probe without a device:**
   ```bash
   python3 scripts/vr_probe.py <videoId>          # VR vs IOS byte-range probe
   python3 scripts/vr_probe.py <videoId> --client VR   # VR only
   ```
   Distinguishes outright-403, ~1-min teaser-block, and throttling.

5. **Reference extractors** (checked out in-repo): `yt-dlp/yt_dlp/extractor/youtube/` (esp. `_base.py` `INNERTUBE_CLIENTS` and PO-token policies) and `NewPipeExtractor/extractor/`. Check these when YouTube changes client requirements.

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

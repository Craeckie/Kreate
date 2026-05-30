# innertube — vendored git history

This directory was previously a **git submodule** of
<https://gitlab.com/tannguyen047/innertube-kotlin.git> (branch `dev`).

It was integrated into the Kreate repository as plain files on 2026-05-30,
vendored at submodule commit `bdb6b4433d8d60381706dc7fda3d02458d33a43f`
(upstream history plus local test repairs on top).

The standalone git history no longer travels with these files, so the full
submodule log is preserved below for reference and grep-ability.

**Total commits:** 188

---

## Commit index (newest first)

```
bdb6b44 2026-05-30 claude — test: repair live-network tests against current Innertube API
64c858a 2026-03-14 knighthat — allow MusicQueueRenderer.Content to be null
8e7409b 2026-03-04 Tan Nguyen — Merge branch 'renovate/gradle-9.x' into 'dev'
ff0f8f3 2026-03-04 Tan Nguyen — Merge branch 'renovate/ktor' into 'dev'
adaf7e5 2026-03-04 Tan Nguyen — Merge branch 'renovate/kotlin' into 'dev'
aec104d 2026-03-04 Tan Nguyen — Merge branch 'renovate/okhttp3' into 'dev'
d42e0be 2025-12-10 knighthat — replace firstOrNull with firstNotNullOfOrNull
05fbbab 2025-12-07 knighthat — introduce search and search continuation endpoints
42ad462 2025-12-07 knighthat — additional check whether MusicResponsiveListItemRenderer is song by using playlistItemData
51e6031 2025-12-07 knighthat — add continuation list to SearchBody
7eda32f 2025-12-07 knighthat — add musicShelfContinuation to BrowseResponse.ContinuationContents
f435577 2025-12-07 knighthat — allow text and onDeselectedCommand to be null
d6759b9 2025-12-06 knighthat — use ktor as default network request library
5596a5c 2025-12-06 knighthat — move randomString to separate class
66aa439 2025-12-05 knighthat — turn subtitle from String to Runs
51fdd03 2025-12-05 knighthat — add node to get search suggestions
b3ec392 2025-12-02 knighthat — add continuation browse
23c7973 2025-12-02 knighthat — add ContinuationContents to BrowseResponse
d1fa18b 2025-12-02 knighthat — introduce MultiContent for models with list style layout
881d6b2 2025-12-02 knighthat — introduce HomePage
0c94851 2025-12-02 knighthat — unify Section for all items
15b2b80 2025-12-01 knighthat — introduce Visualized for items with thumbnails
656376f 2025-12-01 knighthat — introduce Continued for items with continuations
23a30b2 2025-11-30 knighthat — add chips to SectionListRenderer's header
c07bbaa 2025-12-01 renovate token — chore(deps): update dependency gradle to v9
f014391 2025-12-01 renovate token — chore(deps): update ktor to v3.3.3
2d61f2c 2025-11-24 renovate token — chore(deps): update kotlin to v2.2.21
0a0e93f 2025-11-24 renovate token — chore(deps): update dependency com.squareup.okhttp3:logging-interceptor to v5.3.2
bd03752 2025-11-18 Tan Nguyen — Merge branch 'renovate/gradle-8.x' into 'dev'
a24a650 2025-11-18 Tan Nguyen — Merge branch 'renovate/ktor' into 'dev'
2b11b24 2025-11-18 renovate token — chore(deps): update ktor to v3.3.2
88ae08c 2025-11-18 knighthat — set InnertubeProvider before all tests
1f0cf63 2025-11-18 knighthat — bump junit from 5.13.4 to 6.0.1
ed24649 2025-11-18 knighthat — add kotlin reflect for test discovery
1faf591 2025-11-18 renovate token — chore(deps): update dependency gradle to v8.14.3
6f4e909 2025-11-18 Tan Nguyen — Merge branch 'renovate/configure' into 'dev'
d6ddd65 2025-11-18 renovate token — Add renovate.json
b9de810 2025-11-06 knighthat — add tests for MusicEditablePlaylistDetailHeaderRenderer
68a21fa 2025-11-04 Yehooda Romem — add: MusicEditablePlaylistDetailHeaderRenderer
2a1ca09 2025-11-04 knighthat — allow nullable header
f70f022 2025-11-04 Tan Nguyen — bump version to 2025.11.04
c29f1c9 2025-10-26 knighthat — add more messages when require & requireNotNull statements fail
9cb2378 2025-10-26 knighthat — replace inline format with normal String.format
d126ef4 2025-09-30 knighthat — bump io.ktor from 3.2.3 to 3.3.0
239ca74 2025-09-30 knighthat — bump org.projectlombok:lombok from 1.18.38 to 1.18.42
e9484fe 2025-09-30 knighthat — bump org.jetbrains:annotations from 26.0.2 to 26.0.2-1
7fc5513 2025-09-26 knighthat — introduce function to get song's details
2879953 2025-09-26 knighthat — new model, song's details
14cd0d7 2025-09-26 knighthat — allow BrowserMediaSession to be null
46d307e 2025-09-26 knighthat — replace interfaces with implemented data classes
90f9200 2025-09-26 knighthat — change PrimaryResults.Results.Contents to List<Content>
18c7603 2025-09-26 knighthat — 123
3762b42 2025-09-26 knighthat — add parser for videoOwnerRenderer
ff8bccb 2025-09-26 knighthat — replace with BadgeImpl
698fc6d 2025-09-26 knighthat — split MetadataBadge & MusicInlineBadge
09161d1 2025-09-26 knighthat — add JvmName
585def4 2025-09-25 knighthat — lift nullable from certain values
9457acd 2025-09-25 knighthat — add styleRuns & headerRuns to AttributedDescription
edb297b 2025-09-25 knighthat — add another nesting interface to Owner
b97a55b 2025-09-22 knighthat — allow musicResponsiveListItemRenderer to be null
4372ad0 2025-09-09 knighthat — bump version to 2025.09.09
854d2fb 2025-09-09 knighthat — simple pipeline job to handle versioning & testing
9a48136 2025-09-08 knighthat — convert Innertube to interface for better readability
c80efb6 2025-09-08 knighthat — allow null visitorData
98732c2 2025-09-08 knighthat — remove kotlin test library
de9efd7 2025-09-08 knighthat — migrate from java to java-library
3b4336c 2025-09-08 knighthat — replace okhttp3 with ktor okhttp
bfacafa 2025-09-08 knighthat — add junit platform and bundle of junit5
9c3772e 2025-09-08 knighthat — correctly init playbackContext in PlayerBodyBuilder
aacd913 2025-09-08 knighthat — use lombok for no-arg private constructors
81fdb7d 2025-08-28 knighthat — replace ytmIosPlayer with customizable player fetcher
7b20a76 2025-08-28 knighthat — add & implement playerMicroformatRenderer to Microformat
d81b6ae 2025-08-28 knighthat — add signatureTimestamp to PlayerBody
66aa316 2025-08-28 knighthat — replace generic visitor data with platform-specific ones
8b61e63 2025-08-28 knighthat — add default values to Client
53bc0a9 2025-08-28 knighthat — add TVHTML5_EMBEDDED_PLAYER, ANDROID, and ANDROID_VR contexts
e232a27 2025-08-28 knighthat — remove platform property from Client
cc15119 2025-08-28 knighthat — replace originalUrl with constant YOUTUBE_MUSIC_URL
ce4a5f5 2025-08-25 knighthat — convert library response to playlist, artist, or album depends on pageType
fb5bc7e 2025-08-25 knighthat — remove default Constants.JSON_HEADERS
c32f687 2025-08-16 knighthat — allow playableInEmbed to be null
cd53fc6 2025-08-16 knighthat — implement to obtain IOS stream url from YTM
23c4685 2025-08-16 knighthat — add cpn parameter to PlayerBodyBuilder
0e30ea6 2025-08-16 knighthat — allow musicVideoType and loudnessDb to be null
f20f79d 2025-08-15 knighthat — short-had function to generate context
e3e17b3 2025-08-15 knighthat — make icon nullable
3541a93 2025-08-15 knighthat — make urlCanonical nullable
24ad1bc 2025-07-30 knighthat — bump com.squareup.okhttp3:okhttp3 from 4.12.0 to 5.1.0
324e87f 2025-07-30 knighthat — add license
a6b0e4b 2025-07-30 dependabot[bot] — Bump io.ktor:ktor-serialization-kotlinx-json from 3.1.3 to 3.2.3
9c2d37b 2025-07-30 dependabot[bot] — Bump junit5 from 5.13.2 to 5.13.4
4f0b1a6 2025-07-30 knighthat — add script to notify when dependency updates are available
17620cd 2025-07-22 knighthat — add option to fetch playlist with login credentials
7a82894 2025-07-22 knighthat — fix YT user's playlists sync with new API
7ef97dc 2025-07-22 knighthat — make visitorData nullable
1633918 2025-07-21 knighthat — new endpoint to get account's information
416fce3 2025-07-21 knighthat — add endpoint to retrieve account's information
a732c02 2025-07-21 knighthat — add more fields to extract cookies and dataSyncId
c2bd0e1 2025-07-21 knighthat — add User for login
296116a 2025-07-21 knighthat — add use login for to support YT login
dad50e2 2025-07-21 knighthat — extract visitorData from Innertube.Provider instead of from Constants
636c983 2025-07-21 knighthat — convert nullable client to lateinit
27a00de 2025-07-19 knighthat — implement endpoint to query charts with countries from YTM
3627265 2025-07-19 knighthat — add FormData to BrowseBody for charts
81446ee 2025-07-19 knighthat — introduce InnertubeCharts and its tests
b50a4fb 2025-07-19 knighthat — fixup! use browseId as artistId instead of channelId from header
6e9ed91 2025-07-19 knighthat — add ranked artist for charts
c72f00c 2025-07-19 knighthat — add CustomIndexColumn to MusicResponsiveListItemRenderer for charts ranking
25ab842 2025-07-19 knighthat — adjust scope to internal
223d9c6 2025-07-19 knighthat — extract icon to its own file
bcd0f4f 2025-07-19 knighthat — fix indentation
9eb7b0b 2025-07-19 knighthat — add video response to NextResponseParser
972b466 2025-07-19 knighthat — add Subheader for charts
25e1040 2025-07-18 knighthat — use browseId as artistId instead of channelId from header
20de46e 2025-07-17 knighthat — shorten name
a8d39ca 2025-07-17 knighthat — make NextResponse.PlayerOverlays.PlayerOverlayRenderer.BrowserMediaSession.BrowserMediaSessionRenderer.album nullable
27acc8c 2025-07-13 knighthat — promote dependencies to implementation
28bd35f 2025-07-12 knighthat — make BrowseResponse.Header. MusicImmersiveHeaderRenderer.description nullable
ffad7b8 2025-07-09 knighthat — method to retrieve song's basic info & related songs
2e24911 2025-07-09 knighthat — adjust InnertubeArtistImpl.from to parse non-music channels
7360203 2025-07-09 knighthat — add MusicVisualHeaderRenderer for non-music channels
df3fbf9 2025-07-09 knighthat — add ServiceTracking to response's Context
b9115d3 2025-07-09 knighthat — move parse from InnertubeArtistImpl to InnertubeArtistImpl.SectionImpl from
dbc1985 2025-07-09 knighthat — make BrowseResponse.Header.MusicImmersiveHeaderRenderer.monthlyListenerCount nullable
0186ad8 2025-07-09 knighthat — add MusicQueueRenderer for next song info
c2c1456 2025-07-09 knighthat — make PlaylistPanelRenderer.playlistId & PlaylistPanelRenderer.numItemsToShow nullable
c753918 2025-07-09 knighthat — adjust ytmBrowse to accept context's components to build
4e8c3df 2025-07-08 knighthat — revert! swap innertube-java with innertube-kotlin
e03efa3 2025-07-08 knighthat — fixup! adjust visibility
11cf0b7 2025-07-08 knighthat — fixup! add songs fetcher to album when parse from BrowseResponse
9fb963b 2025-07-08 knighthat — add methods to get album and its songs
62efbec 2025-07-08 knighthat — add songs fetcher to album when parse from BrowseResponse
d127aeb 2025-07-08 knighthat — adjust visibility
64ea9d8 2025-07-08 knighthat — make MusicPlaylistShelfRenderer.playlistId and MusicPlaylistShelfRenderer.targetId nullable
3611558 2025-07-08 knighthat — add kotlinx-coroutines dependency
0465f0c 2025-07-08 knighthat — add urlCanonical, subtitle, songs, and sections to InnertubeAlbum
27f6210 2025-07-08 knighthat — change DSL pageType form Runs.Run to Endpoint
fec22ad 2025-07-07 knighthat — make MusicShelfRenderer.title nullable
2a1c11f 2025-07-07 knighthat — fixup! rename VideoViewCountRenderer to Renderer & merge MusicTastebuilderShelfThumbnailRenderer to Thumbnail.Renderer
d7842ef 2025-07-07 knighthat — add browse endpoint to retrieve artist
83b8e60 2025-07-07 knighthat — turn Provider into functional interface
d71bd23 2025-07-07 knighthat — replace LinkedHashSet.removeFirst() with custom Pair for backward compatibility
8f3c5f9 2025-07-07 knighthat — change BrowseResponse.contents to nullable
0ae0c2f 2025-07-06 knighthat — implement AccessibleViaUrl for sharable urls
d081bda 2025-07-06 knighthat — remove unused code
351066c 2025-07-06 knighthat — fixup! change durationText to nullable & replace word authors with artists
e94ab72 2025-07-06 knighthat — fix typo in doc
e56a74d 2025-07-06 knighthat — remove nullable element from httpMethod
c8a3295 2025-07-06 knighthat — swap innertube-java with innertube-kotlin
12197ce 2025-07-07 Knight Hat — remove dependency-submission
ee2e982 2025-07-07 Knight Hat — Create test-on-push.yml
5da3387 2025-07-06 knighthat — simple request template to Innertube and tests
7008edb 2025-07-06 knighthat — port Request to Kotlin
b7fe120 2025-07-06 knighthat — turn into sealed interface and @Serializable
826872f 2025-07-06 knighthat — remove duplicate repositories & fix spacing
9750a4c 2025-07-06 knighthat — add backup to description extractor
b885076 2025-07-06 knighthat — use correct implementation instead of interface
f7d9170 2025-07-06 knighthat — fix typo
bf9b790 2025-07-05 knighthat — implement parsers & port tests of InnertubeSongImpl
73257b7 2025-07-05 knighthat — change durationText to nullable & replace word authors with artists
6069b5e 2025-07-05 knighthat — implement parsers & port tests of InnertubePlaylistImpl
5190598 2025-07-05 knighthat — implement parsers & port tests of InnertubeArtistImpl
8fc4a0e 2025-07-05 knighthat — implement parsers & port tests of InnertubeAlbumImpl
29862fa 2025-07-05 knighthat — implement parsers & port tests of ContinuedPlaylistImpl
6178ac4 2025-07-05 knighthat — add subtitleText converter
03cb41e 2025-07-05 knighthat — change from var to val
af14d45 2025-07-04 knighthat — convert Section to interface
6bfbfc8 2025-07-04 knighthat — fix reference
1421f4e 2025-07-04 knighthat — add subscription button to BrowseResponse for artist
e3657df 2025-07-04 knighthat — replace Endpoint.Browse with Runs.Run of artists
38aa47a 2025-07-04 knighthat — implementations of models
a249a47 2025-07-04 knighthat — introduce interface for innertube's components
4df215e 2025-07-04 knighthat — add missing icon interface
99c9d49 2025-07-02 knighthat — implement tests for requests body builders
6c4776b 2025-07-02 knighthat — port request bodies to Kotlin
1f1de97 2025-07-02 knighthat — port localization and context
8aa6ef7 2025-07-02 knighthat — add org.junit.jupiter:junit-jupiter-params
78fd6a1 2025-07-02 knighthat — update nullability
1680516 2025-07-02 knighthat — fixup! port implementations of response components
497ca30 2025-07-02 knighthat — basic files
116fa16 2025-07-02 knighthat — add basic response tests
0e9eb83 2025-07-02 knighthat — rename VideoViewCountRenderer to Renderer & merge MusicTastebuilderShelfThumbnailRenderer to Thumbnail.Renderer
2864cf2 2025-07-01 knighthat — port implementations of response components
380e6f4 2025-07-01 knighthat — port response components from java to kotlin
8bb7fb5 2025-07-01 knighthat — add kotlin sdk
8a9a8ef 2025-07-01 knighthat — init commit
3eb1fb9 2025-07-01 knighthat — init commit
1a66afd 2025-07-01 knighthat — init commit
```

---

## Full history (messages + file stats)

```
commit bdb6b4433d8d60381706dc7fda3d02458d33a43f
Author: claude <claude@vreg>
Date:   2026-05-30

    test: repair live-network tests against current Innertube API
    
    The test sources had drifted from the refactored API (Innertube.Provider →
    KtorProvider; removed execute()/sendRequest()/Request.dataToSend/Response(...)),
    so the whole test source set failed to compile and no tests could run.
    
    - Rewrite the InnertubeProvider test helper to implement the current
      KtorProvider (client/cookies/dataSyncId/visitorData), mirroring the app's
      NetworkModule client config, and honour an env proxy when present.
    - Replace the obsolete Java InnertubeImplTest (used the removed sendRequest)
      with a Kotlin live-network test driving the high-level API: search,
      searchSuggestion, songBasicInfo and player against real YouTube Music.
    
    Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>

 .../innertube/response/InnertubeImplTest.java      | 72 ----------------
 .../me/knighthat/innertube/InnertubeProvider.kt    | 99 +++++++---------------
 .../innertube/response/InnertubeImplTest.kt        | 70 +++++++++++++++
 3 files changed, 99 insertions(+), 142 deletions(-)

commit 64c858ad8bcd544a708f77fbdda07cdb57ca3fa8
Author: knighthat <git@knighthat.me>
Date:   2026-03-14

    allow MusicQueueRenderer.Content to be null

 src/main/kotlin/me/knighthat/innertube/response/MusicQueueRenderer.kt   | 2 +-
 .../kotlin/me/knighthat/internal/response/MusicQueueRendererImpl.kt     | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)

commit 8e7409ba68f371ba0d155013d479f2e19d63d5a4
Merge: ff0f8f3 c07bbaa
Author: Tan Nguyen <git@knighthat.me>
Date:   2026-03-04

    Merge branch 'renovate/gradle-9.x' into 'dev'
    
    Update dependency gradle to v9
    
    See merge request tannguyen047/innertube-kotlin!11

commit ff0f8f3ae4f7b3b98adc2eeb9ebda04c45203e01
Merge: adaf7e5 f014391
Author: Tan Nguyen <git@knighthat.me>
Date:   2026-03-04

    Merge branch 'renovate/ktor' into 'dev'
    
    Update ktor to v3.3.3
    
    See merge request tannguyen047/innertube-kotlin!10

commit adaf7e572e120a7d23a094c727ada7e378858b3d
Merge: aec104d 2d61f2c
Author: Tan Nguyen <git@knighthat.me>
Date:   2026-03-04

    Merge branch 'renovate/kotlin' into 'dev'
    
    Update kotlin to v2.2.21
    
    See merge request tannguyen047/innertube-kotlin!9

commit aec104dad0ed4849c328a47373d38b2235fc0a0f
Merge: d42e0be 0a0e93f
Author: Tan Nguyen <git@knighthat.me>
Date:   2026-03-04

    Merge branch 'renovate/okhttp3' into 'dev'
    
    Update dependency com.squareup.okhttp3:logging-interceptor to v5.3.2
    
    See merge request tannguyen047/innertube-kotlin!8

commit d42e0be4db52b7644be30085ea012f24fe9d4d6b
Author: knighthat <git@knighthat.me>
Date:   2025-12-10

    replace firstOrNull with firstNotNullOfOrNull

 .../kotlin/me/knighthat/internal/InnertubeImpl.kt     | 19 +++++++------------
 .../me/knighthat/internal/model/HomePageImpl.kt       |  4 ++--
 .../me/knighthat/internal/model/InnertubeAlbumImpl.kt |  8 ++++----
 .../knighthat/internal/model/InnertubeArtistImpl.kt   |  4 ++--
 .../knighthat/internal/model/InnertubePlaylistImpl.kt |  4 ++--
 .../knighthat/internal/model/InnertubeSearchImpl.kt   |  8 ++++----
 6 files changed, 21 insertions(+), 26 deletions(-)

commit 05fbbab9469317042e9cd4cb80cea7f6b2bce417
Author: knighthat <git@knighthat.me>
Date:   2025-12-07

    introduce search and search continuation endpoints

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 13 +++++
 .../knighthat/innertube/model/InnertubeSearch.kt   |  7 +++
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 59 ++++++++++++++++++++++
 .../internal/model/InnertubePlaylistImpl.kt        | 23 +++++++++
 .../internal/model/InnertubeSearchImpl.kt          | 43 ++++++++++++++++
 .../kotlin/me/knighthat/internal/model/ModelDSL.kt |  1 +
 6 files changed, 146 insertions(+)

commit 42ad46284a1e4c51ca293b85c0ad4a66cdf331b4
Author: knighthat <git@knighthat.me>
Date:   2025-12-07

    additional check whether MusicResponsiveListItemRenderer is song by using playlistItemData

 src/main/kotlin/me/knighthat/internal/model/ModelDSL.kt | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

commit 51e6031ddc000e992e19ddd71a194f99ac938583
Author: knighthat <git@knighthat.me>
Date:   2025-12-07

    add continuation list to SearchBody

 .../innertube/request/body/SearchBodyBuilder.java      | 18 +++++++++++++-----
 .../innertube/request/body/search/Builder.java         |  7 +++++--
 .../me/knighthat/innertube/request/body/SearchBody.kt  |  5 +++--
 .../knighthat/innertube/response/MusicShelfRenderer.kt |  1 +
 .../internal/response/MusicShelfRendererImpl.kt        |  3 ++-
 5 files changed, 24 insertions(+), 10 deletions(-)

commit 7eda32f5db45738b72e5d5f33bc1aa6f86b657dd
Author: knighthat <git@knighthat.me>
Date:   2025-12-07

    add musicShelfContinuation to BrowseResponse.ContinuationContents

 src/main/kotlin/me/knighthat/innertube/response/BrowseResponse.kt   | 6 ++++--
 .../kotlin/me/knighthat/internal/response/BrowseResponseImpl.kt     | 6 ++++--
 2 files changed, 8 insertions(+), 4 deletions(-)

commit f435577e31602614ec9c62bf68f9bd6e455dc888
Author: knighthat <git@knighthat.me>
Date:   2025-12-07

    allow text and onDeselectedCommand to be null

 src/main/kotlin/me/knighthat/innertube/response/ChipCloudRenderer.kt  | 4 ++--
 .../kotlin/me/knighthat/internal/response/ChipCloudRendererImpl.kt    | 4 ++--
 2 files changed, 4 insertions(+), 4 deletions(-)

commit d6759b9915eae1e6043427f0bc72604ec9971038
Author: knighthat <git@knighthat.me>
Date:   2025-12-06

    use ktor as default network request library

 build.gradle.kts                                   |   4 +-
 .../kotlin/me/knighthat/innertube/Innertube.kt     |  44 ++--
 .../me/knighthat/innertube/request/Request.kt      |  35 ---
 .../me/knighthat/innertube/response/Response.kt    |  10 -
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 280 +++++++++------------
 .../me/knighthat/internal/util/ContextUtil.kt      |  42 ++++
 .../me/knighthat/internal/util/CookieUtil.kt       |  24 ++
 7 files changed, 205 insertions(+), 234 deletions(-)

commit 5596a5c430b96436d76ce0af2d1fff83dd5cbda6
Author: knighthat <git@knighthat.me>
Date:   2025-12-06

    move randomString to separate class

 .../me/knighthat/innertube/util/InnertubeUtils.kt       | 17 +++++++++++++++++
 src/main/kotlin/me/knighthat/internal/InnertubeImpl.kt  |  4 ++--
 2 files changed, 19 insertions(+), 2 deletions(-)

commit 66aa439b8411185736f4da80110a436bbaedb21c
Author: knighthat <git@knighthat.me>
Date:   2025-12-05

    turn subtitle from String to Runs

 src/main/kotlin/me/knighthat/innertube/model/InnertubeAlbum.kt   | 2 +-
 .../kotlin/me/knighthat/internal/model/InnertubeAlbumImpl.kt     | 9 +++------
 2 files changed, 4 insertions(+), 7 deletions(-)

commit 51fdd039b94a625da8853fac4e0435f80fd7f699
Author: knighthat <git@knighthat.me>
Date:   2025-12-05

    add node to get search suggestions

 .../kotlin/me/knighthat/innertube/Innertube.kt     |  6 +++
 .../innertube/model/InnertubeSearchSuggestion.kt   | 24 ++++++++++++
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 24 ++++++++++++
 .../internal/model/InnertubeArtistImpl.kt          | 24 ++++++++++++
 .../model/InnertubeSearchSuggestionImpl.kt         | 43 ++++++++++++++++++++++
 .../kotlin/me/knighthat/internal/model/ModelDSL.kt | 13 +++++++
 6 files changed, 134 insertions(+)

commit b3ec3925e18cfee92f6210ba866ccac6b2162902
Author: knighthat <git@knighthat.me>
Date:   2025-12-02

    add continuation browse

 .../kotlin/me/knighthat/innertube/Innertube.kt     |  8 ++++++
 .../innertube/model/InnertubeContinuation.kt       |  4 +++
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 31 ++++++++++++++++++++++
 3 files changed, 43 insertions(+)

commit 23c79735f1b4af2ed15b5a02297a15192e5f9231
Author: knighthat <git@knighthat.me>
Date:   2025-12-02

    add ContinuationContents to BrowseResponse

 src/main/kotlin/me/knighthat/innertube/model/MultiContent.kt      | 2 +-
 src/main/kotlin/me/knighthat/innertube/response/BrowseResponse.kt | 6 ++++++
 .../kotlin/me/knighthat/internal/response/BrowseResponseImpl.kt   | 8 +++++++-
 3 files changed, 14 insertions(+), 2 deletions(-)

commit d1fa18b4154d75d9d6ff17f39c058f46eb30d1b2
Author: knighthat <git@knighthat.me>
Date:   2025-12-02

    introduce MultiContent for models with list style layout

 src/main/kotlin/me/knighthat/innertube/model/HomePage.kt        | 4 +---
 src/main/kotlin/me/knighthat/innertube/model/InnertubeAlbum.kt  | 4 +---
 src/main/kotlin/me/knighthat/innertube/model/InnertubeArtist.kt | 4 +---
 src/main/kotlin/me/knighthat/innertube/model/InnertubeCharts.kt | 3 +--
 src/main/kotlin/me/knighthat/innertube/model/MultiContent.kt    | 7 +++++++
 5 files changed, 11 insertions(+), 11 deletions(-)

commit 881d6b2250ce28f72f53723e214e4379be6d70c1
Author: knighthat <git@knighthat.me>
Date:   2025-12-02

    introduce HomePage

 .../kotlin/me/knighthat/innertube/Innertube.kt     |  3 ++
 .../me/knighthat/innertube/model/HomePage.kt       |  6 +++
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 11 ++++++
 .../me/knighthat/internal/model/HomePageImpl.kt    | 43 ++++++++++++++++++++++
 4 files changed, 63 insertions(+)

commit 0c94851503ddd11686ad3ac35fdf10d038a03625
Author: knighthat <git@knighthat.me>
Date:   2025-12-02

    unify Section for all items

 .../me/knighthat/innertube/model/InnertubeAlbum.kt | 12 ----
 .../knighthat/innertube/model/InnertubeArtist.kt   | 12 ----
 .../knighthat/innertube/model/InnertubeCharts.kt   |  9 ---
 .../kotlin/me/knighthat/innertube/model/Section.kt | 36 ++++++++++
 .../knighthat/internal/model/InnertubeAlbumImpl.kt | 56 +++------------
 .../internal/model/InnertubeArtistImpl.kt          | 79 ++--------------------
 .../internal/model/InnertubeChartsImpl.kt          | 64 ++----------------
 .../internal/model/InnertubeRankedArtistImpl.kt    |  4 +-
 .../kotlin/me/knighthat/internal/model/ModelDSL.kt | 61 +++++++++++++++++
 9 files changed, 121 insertions(+), 212 deletions(-)

commit 15b2b8033603718dfe6fb3bc64edc4c09c08238b
Author: knighthat <git@knighthat.me>
Date:   2025-12-01

    introduce Visualized for items with thumbnails

 src/main/kotlin/me/knighthat/innertube/model/InnertubeItem.kt |  8 +-------
 src/main/kotlin/me/knighthat/innertube/model/Visualized.kt    | 11 +++++++++++
 2 files changed, 12 insertions(+), 7 deletions(-)

commit 656376f1fef966abc4412f17158863f373efb49e
Author: knighthat <git@knighthat.me>
Date:   2025-12-01

    introduce Continued for items with continuations

 .../kotlin/me/knighthat/innertube/model/Continued.kt  | 19 +++++++++++++++++++
 .../me/knighthat/innertube/model/InnertubePlaylist.kt |  7 +------
 2 files changed, 20 insertions(+), 6 deletions(-)

commit 23a30b270761bebad80aeb2d550e1fcf4ab1dda0
Author: knighthat <git@knighthat.me>
Date:   2025-11-30

    add chips to SectionListRenderer's header

 .../innertube/response/ChipCloudRenderer.kt        | 18 +++++++++++++++++
 .../innertube/response/SectionListRenderer.kt      |  6 ++++++
 .../internal/response/ChipCloudRendererImpl.kt     | 23 ++++++++++++++++++++++
 .../internal/response/SectionListRendererImpl.kt   |  6 ++++++
 4 files changed, 53 insertions(+)

commit c07bbaa2d3389faa12c1ab042cfa42fd8c9c5da3
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-12-01

    chore(deps): update dependency gradle to v9

 gradle/wrapper/gradle-wrapper.properties | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

commit f0143911f0a4d82051eef05dc0ee22130a7974ae
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-12-01

    chore(deps): update ktor to v3.3.3

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 2d61f2cd3ad41ca0069a4771f943fd3df4cd6193
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-11-24

    chore(deps): update kotlin to v2.2.21

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 0a0e93fec7eba44d6f37be217cf435ec3a4cca72
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-11-24

    chore(deps): update dependency com.squareup.okhttp3:logging-interceptor to v5.3.2

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit bd0375272628dc52aee81644caf4938f3b1b3c88
Merge: a24a650 1faf591
Author: Tan Nguyen <git@knighthat.me>
Date:   2025-11-18

    Merge branch 'renovate/gradle-8.x' into 'dev'
    
    chore(deps): update dependency gradle to v8.14.3
    
    See merge request tannguyen047/innertube-kotlin!6

commit a24a650d524ba3e68dbf4e0d19abdc7c5d6b6d6d
Merge: 88ae08c 2b11b24
Author: Tan Nguyen <git@knighthat.me>
Date:   2025-11-18

    Merge branch 'renovate/ktor' into 'dev'
    
    chore(deps): update ktor to v3.3.2
    
    See merge request tannguyen047/innertube-kotlin!7

commit 2b11b24987f2169eea621f047a08afc63610a802
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-11-18

    chore(deps): update ktor to v3.3.2

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 88ae08c0865455ec328e26a908a1eb71dfc2ff3c
Author: knighthat <git@knighthat.me>
Date:   2025-11-18

    set InnertubeProvider before all tests

 .../kotlin/me/knighthat/internal/model/InnertubeAlbumImplTest.kt | 9 +++++++++
 1 file changed, 9 insertions(+)

commit 1f0cf6313b842dd901c4e850799681898236d184
Author: knighthat <git@knighthat.me>
Date:   2025-11-18

    bump junit from 5.13.4 to 6.0.1

 gradle/libs.versions.toml | 5 ++---
 1 file changed, 2 insertions(+), 3 deletions(-)

commit ed246498f3d6e299895c5080eeff7b7672981006
Author: knighthat <git@knighthat.me>
Date:   2025-11-18

    add kotlin reflect for test discovery

 build.gradle.kts          | 1 +
 gradle/libs.versions.toml | 1 +
 2 files changed, 2 insertions(+)

commit 1faf59165c7676ac1b854ec2e119e9a4fc88eb57
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-11-18

    chore(deps): update dependency gradle to v8.14.3

 gradle/wrapper/gradle-wrapper.properties | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

commit 6f4e909f9be5d1a96b3775465616de523f7760fe
Merge: b9de810 d6ddd65
Author: Tan Nguyen <git@knighthat.me>
Date:   2025-11-18

    Merge branch 'renovate/configure' into 'dev'
    
    chore: Configure Renovate
    
    See merge request tannguyen047/innertube-kotlin!5

commit d6ddd6515ed75a8a3c8ef89124cee2587d1dcc70
Author: renovate token <project_71295828_bot_34297000d36ef24470a3140a1c95792f@noreply.gitlab.com>
Date:   2025-11-18

    Add renovate.json

 renovate.json | 3 +++
 1 file changed, 3 insertions(+)

commit b9de810779e1cdb79b6c2d16e1ae53a614e9c0a4
Author: knighthat <git@knighthat.me>
Date:   2025-11-06

    add tests for MusicEditablePlaylistDetailHeaderRenderer

 .../internal/model/InnertubePlaylistImplTest.kt    |    14 +-
 .../playlist_twoColumnBrowseResultsRenderer4.json  | 46937 +++++++++++++------
 2 files changed, 32742 insertions(+), 14209 deletions(-)

commit 68a21fae3873138b5823340c88926fa4e9872d31
Author: Yehooda Romem <yrrad8@gmail.com>
Date:   2025-11-04

    add: MusicEditablePlaylistDetailHeaderRenderer

 .../innertube/response/SectionListRenderer.kt      | 11 +++++++
 .../internal/model/InnertubePlaylistImpl.kt        | 38 ++++++++++++----------
 .../internal/response/SectionListRendererImpl.kt   | 12 +++++++
 3 files changed, 44 insertions(+), 17 deletions(-)

commit 2a1ca09436e6034a2fe40040e72e76946ea5c32a
Author: knighthat <git@knighthat.me>
Date:   2025-11-04

    allow nullable header

 .../internal/model/InnertubePlaylistImpl.kt        |    16 +-
 .../internal/model/InnertubePlaylistImplTest.kt    |    11 +
 .../playlist_twoColumnBrowseResultsRenderer4.json  | 43952 +++++++++++++++++++
 3 files changed, 43972 insertions(+), 7 deletions(-)

commit f70f022d497528899b20063415b350159c7899bb
Author: Tan Nguyen <git@knighthat.me>
Date:   2025-11-04

    bump version to 2025.11.04

 build.gradle.kts | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit c29f1c99d69096cdbd246f09366dc6ced8ca1556
Author: knighthat <git@knighthat.me>
Date:   2025-10-26

    add more messages when require & requireNotNull statements fail

 src/main/kotlin/me/knighthat/internal/InnertubeImpl.kt   |  6 +++---
 .../me/knighthat/internal/model/InnertubeAlbumImpl.kt    | 10 +++++++---
 .../me/knighthat/internal/model/InnertubeArtistImpl.kt   | 16 +++++++++-------
 .../internal/model/InnertubeRankedArtistImpl.kt          | 14 ++++++++------
 4 files changed, 27 insertions(+), 19 deletions(-)

commit 9cb23782798347dd88fc0a6474af04b0f2da94e6
Author: knighthat <git@knighthat.me>
Date:   2025-10-26

    replace inline format with normal String.format

 src/main/kotlin/me/knighthat/internal/model/InnertubeAlbumImpl.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit d126ef417c74c1e6fe30263953f31ffb715b9d67
Author: knighthat <git@knighthat.me>
Date:   2025-09-30

    bump io.ktor from 3.2.3 to 3.3.0

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 239ca7421a214c9e4fa9beb7fdefd3ed63f3379d
Author: knighthat <git@knighthat.me>
Date:   2025-09-30

    bump org.projectlombok:lombok from 1.18.38 to 1.18.42

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit e9484fe480c5167237dbe67075a54dfd749346c4
Author: knighthat <git@knighthat.me>
Date:   2025-09-30

    bump org.jetbrains:annotations from 26.0.2 to 26.0.2-1

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 7fc5513e61eda0a9496fcd12e0731db5b77fa2ba
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    introduce function to get song's details

 .../kotlin/me/knighthat/innertube/Innertube.kt     |  3 +++
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 26 ++++++++++++++++++++++
 2 files changed, 29 insertions(+)

commit 28799538b56ca0c1a151cfe37593fd3c5e194cd6
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    new model, song's details

 .../innertube/model/InnertubeSongDetails.kt        |    31 +
 .../internal/model/InnertubeSongDetailsImpl.kt     |    46 +
 .../innertube/response/NextResponseTest.kt         |     3 +-
 .../internal/model/InnertubeSongDetailsImplTest.kt |    84 +
 src/test/resources/yt/next/endpoint_response.json  | 19961 +++++++++++++++++++
 5 files changed, 20124 insertions(+), 1 deletion(-)

commit 14cd0d7548a2131c6735254de44891066593fafb
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    allow BrowserMediaSession to be null

 src/main/kotlin/me/knighthat/innertube/response/NextResponse.kt    | 4 ++--
 src/main/kotlin/me/knighthat/internal/response/NextResponseImpl.kt | 2 +-
 2 files changed, 3 insertions(+), 3 deletions(-)

commit 46d307eca6a2e433c2bdcdde8ed4c18b8df6f6e8
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    replace interfaces with implemented data classes

 .../kotlin/me/knighthat/internal/response/NextResponseImpl.kt    | 3 +--
 .../kotlin/me/knighthat/internal/response/PrimaryResultsImpl.kt  | 9 ++++-----
 2 files changed, 5 insertions(+), 7 deletions(-)

commit 90f92006c003ec7e3350d5f1aa83583f94a224be
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    change PrimaryResults.Results.Contents to List<Content>

 .../internal/response/PrimaryResultsImpl.kt        | 32 +++++++++++-----------
 1 file changed, 16 insertions(+), 16 deletions(-)

commit 18c7603ae4c703238486877de5da8524ad02f978
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    123

 src/main/kotlin/me/knighthat/innertube/response/PrimaryResults.kt     | 4 ++--
 src/main/kotlin/me/knighthat/internal/model/InnertubeArtistImpl.kt    | 2 +-
 .../kotlin/me/knighthat/internal/model/InnertubeArtistImplTest.kt     | 2 +-
 3 files changed, 4 insertions(+), 4 deletions(-)

commit 3762b4211bee22ee1b7bcc3f4e0540be09e884e5
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    add parser for videoOwnerRenderer

 .../internal/model/InnertubeArtistImpl.kt          | 18 +++++
 .../internal/model/InnertubeArtistImplTest.kt      | 24 ++++++
 .../yt/next/artist_videoOwnerRenderer.json         | 85 ++++++++++++++++++++++
 3 files changed, 127 insertions(+)

commit ff8bccb183c6bceafe944460fb1584803327afaa
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    replace with BadgeImpl

 src/main/kotlin/me/knighthat/internal/response/PrimaryResultsImpl.kt | 3 +--
 1 file changed, 1 insertion(+), 2 deletions(-)

commit 698fc6d00e79dc3d295d3ee3410f601299164411
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    split MetadataBadge & MusicInlineBadge

 .../me/knighthat/innertube/response/Badge.kt       | 15 ++++++++++---
 .../me/knighthat/internal/response/BadgeImpl.kt    | 25 +++++++++++-----------
 2 files changed, 25 insertions(+), 15 deletions(-)

commit 09161d13e457d75bcc3c93aa90c122d22169bbcd
Author: knighthat <git@knighthat.me>
Date:   2025-09-26

    add JvmName

 src/main/kotlin/me/knighthat/internal/InnertubeImpl.kt               | 1 +
 src/test/java/me/knighthat/innertube/response/InnertubeImplTest.java | 4 ++--
 2 files changed, 3 insertions(+), 2 deletions(-)

commit 585def46234ea8a897d32039102eca06193e3c1b
Author: knighthat <git@knighthat.me>
Date:   2025-09-25

    lift nullable from certain values

 .../knighthat/innertube/response/PrimaryResults.kt   | 20 ++++++++++----------
 .../internal/response/PrimaryResultsImpl.kt          | 20 ++++++++++----------
 2 files changed, 20 insertions(+), 20 deletions(-)

commit 9457acd17a742eb6971389786c1b16012bf1a4df
Author: knighthat <git@knighthat.me>
Date:   2025-09-25

    add styleRuns & headerRuns to AttributedDescription

 .../knighthat/innertube/response/PrimaryResults.kt | 29 +++++++++++++++
 .../internal/response/PrimaryResultsImpl.kt        | 43 +++++++++++++++++++++-
 2 files changed, 70 insertions(+), 2 deletions(-)

commit edb297b806855c745d3f83ff78fc9300bc88b1cc
Author: knighthat <git@knighthat.me>
Date:   2025-09-25

    add another nesting interface to Owner

 .../knighthat/innertube/response/PrimaryResults.kt   | 17 ++++++++++-------
 .../internal/response/PrimaryResultsImpl.kt          | 20 +++++++++++---------
 2 files changed, 21 insertions(+), 16 deletions(-)

commit b97a55b2715f5d94495b4f386b367afe3f953595
Author: knighthat <git@knighthat.me>
Date:   2025-09-22

    allow musicResponsiveListItemRenderer to be null

 src/main/kotlin/me/knighthat/innertube/response/MusicShelfRenderer.kt   | 2 +-
 src/main/kotlin/me/knighthat/internal/model/InnertubeArtistImpl.kt      | 2 +-
 .../kotlin/me/knighthat/internal/response/MusicShelfRendererImpl.kt     | 2 +-
 3 files changed, 3 insertions(+), 3 deletions(-)

commit 4372ad01a5334271b7c234cb8b99978d7de3ffa1
Author: knighthat <git@knighthat.me>
Date:   2025-09-09

    bump version to 2025.09.09

 build.gradle.kts | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 854d2fbfe330d6426d21049a2676afd394032cfb
Author: knighthat <git@knighthat.me>
Date:   2025-09-09

    simple pipeline job to handle versioning & testing

 .gitlab-ci.yml | 56 ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 1 file changed, 56 insertions(+)

commit 9a481369bfab63e61b6b9fcdda9550592cde0ea7
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    convert Innertube to interface for better readability

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 406 ++------------------
 .../kotlin/me/knighthat/internal/InnertubeImpl.kt  | 413 +++++++++++++++++++++
 .../innertube/response/InnertubeImplTest.java      |  72 ++++
 .../me/knighthat/innertube/InnertubeProvider.kt    |  89 +++++
 .../kotlin/me/knighthat/innertube/InnertubeTest.kt | 144 -------
 5 files changed, 596 insertions(+), 528 deletions(-)

commit c80efb6cd133955683cfce9e35ac19903319f6e2
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    allow null visitorData

 src/main/kotlin/me/knighthat/innertube/request/body/Context.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 98732c2469129ff3690d7eb93f44e45120c16c8a
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    remove kotlin test library

 build.gradle.kts                                                      | 1 -
 src/test/kotlin/me/knighthat/internal/model/InnertubeAlbumImplTest.kt | 2 +-
 2 files changed, 1 insertion(+), 2 deletions(-)

commit de9efd7611581422846d31e758fcd58969365c25
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    migrate from java to java-library

 build.gradle.kts | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

commit 3b4336cdba9f6ae7bf8891472d5897c185b48592
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    replace okhttp3 with ktor okhttp

 build.gradle.kts                                   |   3 +-
 gradle/libs.versions.toml                          |   8 +-
 .../kotlin/me/knighthat/innertube/InnertubeTest.kt | 122 +++++++++++----------
 3 files changed, 72 insertions(+), 61 deletions(-)

commit bfacafac26110f7c0925195ca01c23e184f54831
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    add junit platform and bundle of junit5

 build.gradle.kts          | 6 +++---
 gradle/libs.versions.toml | 8 +++++++-
 2 files changed, 10 insertions(+), 4 deletions(-)

commit 9c3772e5a629ecd4706cbea4b173179ed1d46e10
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    correctly init playbackContext in PlayerBodyBuilder

 .../innertube/request/body/PlayerBodyBuilder.java     | 19 ++++++++++++-------
 .../innertube/request/body/player/Builder.java        | 17 ++++++++++++++++-
 src/main/kotlin/me/knighthat/innertube/Innertube.kt   |  3 ++-
 3 files changed, 30 insertions(+), 9 deletions(-)

commit aacd913f9229ecd818028161b934a22030721442
Author: knighthat <git@knighthat.me>
Date:   2025-09-08

    use lombok for no-arg private constructors

 src/main/java/me/knighthat/innertube/Constants.java    | 7 +++++--
 src/main/java/me/knighthat/innertube/Endpoints.java    | 1 +
 src/main/java/me/knighthat/innertube/PageType.java     | 5 +++++
 src/main/java/me/knighthat/innertube/SearchFilter.java | 7 +++++--
 src/main/java/me/knighthat/innertube/UserAgents.java   | 7 +++++--
 5 files changed, 21 insertions(+), 6 deletions(-)

commit 81fdb7d358a5038d6f0de248245646953d6f8aa6
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    replace ytmIosPlayer with customizable player fetcher

 .../java/me/knighthat/innertube/Constants.java     |  3 ++
 .../kotlin/me/knighthat/innertube/Innertube.kt     | 50 +++++++++++++---------
 2 files changed, 33 insertions(+), 20 deletions(-)

commit 7b20a76103fefde4513a312cb332bc37386a1bcf
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    add & implement playerMicroformatRenderer to Microformat

 .../me/knighthat/innertube/response/Microformat.kt |   35 +-
 .../knighthat/internal/response/MicroformatImpl.kt |   49 +-
 .../innertube/response/PlayerResponseTest.kt       |    2 +-
 ...point_response.json => endpoint_response1.json} |    0
 .../resources/ytm/player/endpoint_response2.json   | 1167 ++++++++++++++++++++
 5 files changed, 1242 insertions(+), 11 deletions(-)

commit d81b6ae91b688fb0af79143f6107a89ef5679a8e
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    add signatureTimestamp to PlayerBody

 .../innertube/request/body/PlayerBodyBuilder.java          | 14 +++++++++++++-
 .../knighthat/innertube/request/body/player/Builder.java   |  3 +++
 .../me/knighthat/innertube/request/body/PlayerBody.kt      |  7 +++++++
 3 files changed, 23 insertions(+), 1 deletion(-)

commit 66aa3160fc84f77906c52934e6d806d1a9c15867
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    replace generic visitor data with platform-specific ones

 src/main/java/me/knighthat/innertube/Constants.java        | 14 +++++++++++++-
 .../kotlin/me/knighthat/innertube/request/body/Context.kt  | 12 ++++++------
 src/test/kotlin/me/knighthat/innertube/InnertubeTest.kt    |  2 +-
 .../knighthat/internal/model/InnertubePlaylistImplTest.kt  |  5 ++---
 4 files changed, 22 insertions(+), 11 deletions(-)

commit 8b61e635fcbaacd3f823df9504457a42753ae698
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    add default values to Client

 .../me/knighthat/innertube/request/body/Context.kt | 187 +++++++--------------
 1 file changed, 61 insertions(+), 126 deletions(-)

commit 53bc0a9d39b5bd9b90c36f317ffc3533d0efa1bb
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    add TVHTML5_EMBEDDED_PLAYER, ANDROID, and ANDROID_VR contexts

 .../java/me/knighthat/innertube/UserAgents.java    |  9 ++++
 .../me/knighthat/innertube/request/body/Context.kt | 63 ++++++++++++++++++++++
 2 files changed, 72 insertions(+)

commit e232a273aed95d4cd351ccabd9335d4d7e5cac0b
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    remove platform property from Client

 src/main/kotlin/me/knighthat/innertube/request/body/Context.kt | 6 ------
 1 file changed, 6 deletions(-)

commit cc1511931ba56b01fd26d0af929e9c32ab9182f3
Author: knighthat <git@knighthat.me>
Date:   2025-08-28

    replace originalUrl with constant YOUTUBE_MUSIC_URL

 src/main/kotlin/me/knighthat/innertube/request/body/Context.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit ce4a5f5cf441389cbe6fc7fb112fc63332d1b273
Author: knighthat <git@knighthat.me>
Date:   2025-08-25

    convert library response to playlist, artist, or album depends on pageType

 src/main/kotlin/me/knighthat/innertube/Innertube.kt | 18 ++++++++++++++++--
 1 file changed, 16 insertions(+), 2 deletions(-)

commit fb5bc7e99dc90498e9501c1e582d342ba4d514a9
Author: knighthat <git@knighthat.me>
Date:   2025-08-25

    remove default Constants.JSON_HEADERS
    
    values defined in this map must be added by dev to ensure maximum compatibility

 src/main/java/me/knighthat/innertube/Constants.java   | 11 -----------
 src/main/kotlin/me/knighthat/innertube/Innertube.kt   | 16 +++++++++++++---
 .../kotlin/me/knighthat/innertube/InnertubeTest.kt    | 19 +++++++++----------
 3 files changed, 22 insertions(+), 24 deletions(-)

commit c32f687e31b836d058180ea41afc1b7155072327
Author: knighthat <git@knighthat.me>
Date:   2025-08-16

    allow playableInEmbed to be null

 src/main/kotlin/me/knighthat/innertube/response/PlayerResponse.kt    | 2 +-
 src/main/kotlin/me/knighthat/internal/response/PlayerResponseImpl.kt | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)

commit cd53fc6ca0e50163c245d757ad2015fe9465f852
Author: knighthat <git@knighthat.me>
Date:   2025-08-16

    implement to obtain IOS stream url from YTM

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 34 ++++++++++++++++++++++
 1 file changed, 34 insertions(+)

commit 23c4685eb15cd0abf2f4f5e6dffb0e5bddb64bd4
Author: knighthat <git@knighthat.me>
Date:   2025-08-16

    add cpn parameter to PlayerBodyBuilder

 .../me/knighthat/innertube/request/body/PlayerBodyBuilder.java     | 7 +++++++
 .../java/me/knighthat/innertube/request/body/player/Builder.java   | 5 ++++-
 2 files changed, 11 insertions(+), 1 deletion(-)

commit 0e30ea63277185634d265b0843966cbcd594b44d
Author: knighthat <git@knighthat.me>
Date:   2025-08-16

    allow musicVideoType and loudnessDb to be null

 src/main/kotlin/me/knighthat/innertube/response/PlayerResponse.kt    | 4 ++--
 src/main/kotlin/me/knighthat/internal/response/PlayerResponseImpl.kt | 4 ++--
 2 files changed, 4 insertions(+), 4 deletions(-)

commit f20f79d4ed79a2a7a6471abeee2eb9d8f4755d09
Author: knighthat <git@knighthat.me>
Date:   2025-08-15

    short-had function to generate context

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 44 ++++++++++++----------
 1 file changed, 24 insertions(+), 20 deletions(-)

commit e3e17b33076b388a025de03f7c1ca0fecb8fbcbd
Author: knighthat <git@knighthat.me>
Date:   2025-08-15

    make icon nullable

 src/main/kotlin/me/knighthat/innertube/model/InnertubeRankedArtist.kt | 2 +-
 .../knighthat/innertube/response/MusicResponsiveListItemRenderer.kt   | 2 +-
 .../kotlin/me/knighthat/internal/model/InnertubeRankedArtistImpl.kt   | 4 ++--
 .../internal/response/MusicResponsiveListItemRendererImpl.kt          | 2 +-
 4 files changed, 5 insertions(+), 5 deletions(-)

commit 3541a93862a8c75ba2c7b75e0c64382e64176e80
Author: knighthat <git@knighthat.me>
Date:   2025-08-15

    make urlCanonical nullable

 src/main/kotlin/me/knighthat/innertube/response/Microformat.kt    | 2 +-
 src/main/kotlin/me/knighthat/internal/response/MicroformatImpl.kt | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)

commit 24ad1bcaf4f8285ae9132bb887046938097d6e73
Author: knighthat <git@knighthat.me>
Date:   2025-07-30

    bump com.squareup.okhttp3:okhttp3 from 4.12.0 to 5.1.0
    
    includes:
    - com.squareup.okhttp3:okhttp3
    - com.squareup.okhttp3:logging-interceptor

 gradle/libs.versions.toml                               | 2 +-
 src/test/kotlin/me/knighthat/innertube/InnertubeTest.kt | 4 ++++
 2 files changed, 5 insertions(+), 1 deletion(-)

commit 324e87fe46c91b9a9381f11616a340ae788baf33
Author: knighthat <git@knighthat.me>
Date:   2025-07-30

    add license

 LICENSE | 13 +++++++++++++
 1 file changed, 13 insertions(+)

commit a6b0e4bb649a869892790795edca13095c625172
Author: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
Date:   2025-07-30

    Bump io.ktor:ktor-serialization-kotlinx-json from 3.1.3 to 3.2.3
    
    Bumps [io.ktor:ktor-serialization-kotlinx-json](https://github.com/ktorio/ktor) from 3.1.3 to 3.2.3.
    - [Release notes](https://github.com/ktorio/ktor/releases)
    - [Changelog](https://github.com/ktorio/ktor/blob/main/CHANGELOG.md)
    - [Commits](https://github.com/ktorio/ktor/compare/3.1.3...3.2.3)
    
    ---
    updated-dependencies:
    - dependency-name: io.ktor:ktor-serialization-kotlinx-json
      dependency-version: 3.2.3
      dependency-type: direct:production
      update-type: version-update:semver-minor
    ...
    
    Signed-off-by: dependabot[bot] <support@github.com>

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 9c2d37bf8043a6f737ccb5d477d8509c9e2592be
Author: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>
Date:   2025-07-30

    Bump junit5 from 5.13.2 to 5.13.4
    
    Bumps `junit5` from 5.13.2 to 5.13.4.
    
    Updates `org.junit.jupiter:junit-jupiter-api` from 5.13.2 to 5.13.4
    - [Release notes](https://github.com/junit-team/junit-framework/releases)
    - [Commits](https://github.com/junit-team/junit-framework/compare/r5.13.2...r5.13.4)
    
    Updates `org.junit.jupiter:junit-jupiter-engine` from 5.13.2 to 5.13.4
    - [Release notes](https://github.com/junit-team/junit-framework/releases)
    - [Commits](https://github.com/junit-team/junit-framework/compare/r5.13.2...r5.13.4)
    
    Updates `org.junit.jupiter:junit-jupiter-params` from 5.13.2 to 5.13.4
    - [Release notes](https://github.com/junit-team/junit-framework/releases)
    - [Commits](https://github.com/junit-team/junit-framework/compare/r5.13.2...r5.13.4)
    
    ---
    updated-dependencies:
    - dependency-name: org.junit.jupiter:junit-jupiter-api
      dependency-version: 5.13.4
      dependency-type: direct:production
      update-type: version-update:semver-patch
    - dependency-name: org.junit.jupiter:junit-jupiter-engine
      dependency-version: 5.13.4
      dependency-type: direct:production
      update-type: version-update:semver-patch
    - dependency-name: org.junit.jupiter:junit-jupiter-params
      dependency-version: 5.13.4
      dependency-type: direct:production
      update-type: version-update:semver-patch
    ...
    
    Signed-off-by: dependabot[bot] <support@github.com>

 gradle/libs.versions.toml | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 4f0b1a69f7ff0ae4f236e9c0a3c70736f8683ca7
Author: knighthat <git@knighthat.me>
Date:   2025-07-30

    add script to notify when dependency updates are available

 .github/dependabot.yml | 22 ++++++++++++++++++++++
 1 file changed, 22 insertions(+)

commit 17620cd67d1bf347ee93bc919edebf1e453088f1
Author: knighthat <git@knighthat.me>
Date:   2025-07-22

    add option to fetch playlist with login credentials

 src/main/kotlin/me/knighthat/innertube/Innertube.kt | 12 ++++++++----
 1 file changed, 8 insertions(+), 4 deletions(-)

commit 7a828946d610f86de6073d9cb59697d139ce7e24
Author: knighthat <git@knighthat.me>
Date:   2025-07-22

    fix YT user's playlists sync with new API

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 28 ++++++++++++++++++++++
 1 file changed, 28 insertions(+)

commit 7ef97dcb67138368e138b5dc1815297260807a9e
Author: knighthat <git@knighthat.me>
Date:   2025-07-22

    make visitorData nullable

 src/main/kotlin/me/knighthat/innertube/Innertube.kt                  | 2 +-
 src/main/kotlin/me/knighthat/internal/model/InnertubePlaylistImpl.kt | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)

commit 1633918557ac0dd19d186f33eca59240aa20221a
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    new endpoint to get account's information
    
    used right after login

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 46 ++++++++++++++++++++++
 1 file changed, 46 insertions(+)

commit 416fce3f9737ba59778539730bdc61d02b3af43c
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    add endpoint to retrieve account's information

 .../java/me/knighthat/innertube/Endpoints.java     |  5 ++++-
 .../me/knighthat/innertube/model/AccountInfo.kt    | 11 ++++++++++
 .../innertube/request/body/AccountMenuBody.kt      | 10 +++++++++
 .../response/ActiveAccountHeaderRenderer.kt        |  9 ++++++++
 .../me/knighthat/internal/model/AccountInfoImpl.kt | 24 ++++++++++++++++++++++
 .../response/ActiveAccountHeaderRendererImpl.kt    | 12 +++++++++++
 6 files changed, 70 insertions(+), 1 deletion(-)

commit a732c0288a1fa8e2ec7b8889ff48e2bb313727f6
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    add more fields to extract cookies and dataSyncId

 src/main/kotlin/me/knighthat/innertube/Innertube.kt     | 2 ++
 src/test/kotlin/me/knighthat/innertube/InnertubeTest.kt | 2 ++
 2 files changed, 4 insertions(+)

commit c2bd0e14a1c61fdd5686404af23e3fa64133e7e0
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    add User for login

 src/main/kotlin/me/knighthat/innertube/Innertube.kt     |  6 ++++++
 .../me/knighthat/innertube/request/body/Context.kt      | 17 +++++++++++++----
 2 files changed, 19 insertions(+), 4 deletions(-)

commit 296116ac2146e5dcd15d991babcb720d55d712e8
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    add use login for to support YT login

 src/main/kotlin/me/knighthat/innertube/Innertube.kt      | 16 ++++++++++++++--
 .../kotlin/me/knighthat/innertube/request/Request.kt     |  1 +
 src/test/kotlin/me/knighthat/innertube/InnertubeTest.kt  | 14 ++++++++++++--
 3 files changed, 27 insertions(+), 4 deletions(-)

commit dad50e2d1eccafa5fe0ca1f0a18b02d8724dfdde
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    extract visitorData from Innertube.Provider instead of from Constants

 src/main/kotlin/me/knighthat/innertube/Innertube.kt     | 15 +++++++++------
 src/test/kotlin/me/knighthat/innertube/InnertubeTest.kt |  1 +
 2 files changed, 10 insertions(+), 6 deletions(-)

commit 636c9835c831e5cd2fbcd0900285448a7e98d678
Author: knighthat <git@knighthat.me>
Date:   2025-07-21

    convert nullable client to lateinit

 src/main/kotlin/me/knighthat/innertube/Innertube.kt | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

commit 27a00de21864f58a3328431951e56d7400fde97f
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    implement endpoint to query charts with countries from YTM

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 24 ++++++++++++++++++++++
 1 file changed, 24 insertions(+)

commit 3627265332f683189d9b2d068828d08908b96458
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    add FormData to BrowseBody for charts

 .../innertube/request/body/BrowseBodyBuilder.java  | 24 +++++++++++++++-------
 .../knighthat/innertube/request/body/Builder.java  |  9 ++++++++
 .../innertube/request/body/NextBodyBuilder.java    | 12 ++++++++---
 .../innertube/request/body/PlayerBodyBuilder.java  | 14 +++++++++----
 .../request/body/SearchSuggestionsBodyBuilder.java |  9 +++++++-
 .../knighthat/innertube/request/body/BrowseBody.kt |  1 +
 .../knighthat/innertube/request/body/FormData.kt   |  8 ++++++++
 7 files changed, 62 insertions(+), 15 deletions(-)

commit 81446ee3da587a238b5219d6ea32c68656051fc7
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    introduce InnertubeCharts and its tests

 .../knighthat/innertube/model/InnertubeCharts.kt   |    30 +
 .../internal/model/InnertubeChartsImpl.kt          |   163 +
 .../internal/model/InnertubeChartsImplTest.kt      |    29 +
 .../ytm/browse/charts_sectionListRenderer.json     | 17690 +++++++++++++++++++
 4 files changed, 17912 insertions(+)

commit b50a4fb0f6bb3622a7e87baaab81c2e297e7b119
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    fixup! use browseId as artistId instead of channelId from header

 src/test/kotlin/me/knighthat/internal/model/InnertubeArtistImplTest.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 6e9ed91fda6d7e2955cdfbefbef5addc5de73cd7
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    add ranked artist for charts

 .../innertube/model/InnertubeRankedArtist.kt       |  18 ++
 .../internal/model/InnertubeRankedArtistImpl.kt    |  59 ++++++
 .../model/InnertubeRankedArtistImplTest.kt         | 107 ++++++++++
 ...t_ranking_musicResponsiveListItemRenderer1.json | 229 +++++++++++++++++++++
 ...t_ranking_musicResponsiveListItemRenderer2.json | 229 +++++++++++++++++++++
 ...t_ranking_musicResponsiveListItemRenderer3.json | 229 +++++++++++++++++++++
 6 files changed, 871 insertions(+)

commit c72f00ce16da5f6308025f5b818d952b3a05e636
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    add CustomIndexColumn to MusicResponsiveListItemRenderer for charts ranking

 .../response/MusicResponsiveListItemRenderer.kt          | 13 +++++++++++++
 .../response/MusicResponsiveListItemRendererImpl.kt      | 16 +++++++++++++++-
 2 files changed, 28 insertions(+), 1 deletion(-)

commit 25ab84272b951fe7b958b9f7c2d00e64b909a42b
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    adjust scope to internal

 .../internal/response/MusicResponsiveListItemRendererImpl.kt        | 6 +++---
 1 file changed, 3 insertions(+), 3 deletions(-)

commit 223d9c6835eba5a22ceb496482fe5f22623368d1
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    extract icon to its own file

 src/main/kotlin/me/knighthat/innertube/response/Badge.kt    | 5 -----
 src/main/kotlin/me/knighthat/innertube/response/Icon.kt     | 6 ++++++
 src/main/kotlin/me/knighthat/internal/response/BadgeImpl.kt | 3 ++-
 src/main/kotlin/me/knighthat/internal/response/IconImpl.kt  | 9 +++++++++
 4 files changed, 17 insertions(+), 6 deletions(-)

commit bcd0f4fe384cecaeb6f88e2dcc78bf9f2d035e18
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    fix indentation

 .../internal/model/InnertubeArtistImpl.kt          | 82 +++++++++++-----------
 1 file changed, 41 insertions(+), 41 deletions(-)

commit 9eb7b0ba136241bd84095f944964186030ed4871
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    add video response to NextResponseParser

 .../response/MusicMultiSelectMenuItemRenderer.kt   |   42 -
 .../response/MusicMultiSelectMenuRenderer.kt       |   36 +
 .../innertube/response/MusicShelfRenderer.kt       |    8 +-
 .../MusicMultiSelectMenuItemRendererImpl.kt        |   49 -
 .../response/MusicMultiSelectMenuRendererImpl.kt   |   42 +
 .../internal/response/MusicShelfRendererImpl.kt    |   10 +-
 .../innertube/response/NextResponseTest.kt         |    5 +-
 .../ytm/next/endpoint_video_response.json          | 1148 ++++++++++++++++++++
 8 files changed, 1245 insertions(+), 95 deletions(-)

commit 972b466c7df36d20a2931aafb06983538de3fb69
Author: knighthat <git@knighthat.me>
Date:   2025-07-19

    add Subheader for charts

 .../response/MusicMultiSelectMenuItemRenderer.kt   |    42 +
 .../innertube/response/MusicShelfRenderer.kt       |    24 +-
 .../MusicMultiSelectMenuItemRendererImpl.kt        |    49 +
 .../internal/response/MusicShelfRendererImpl.kt    |    28 +-
 .../innertube/response/BrowseResponseTest.kt       |     3 +-
 .../ytm/browse/endpoint_charts_response.json       | 15138 +++++++++++++++++++
 6 files changed, 15281 insertions(+), 3 deletions(-)

commit 25e10407274b3f55a6d0cf555e16247985b1d9ea
Author: knighthat <git@knighthat.me>
Date:   2025-07-18

    use browseId as artistId instead of channelId from header

 .../me/knighthat/internal/model/InnertubeArtistImpl.kt     | 14 ++++++--------
 1 file changed, 6 insertions(+), 8 deletions(-)

commit 20de46e721aa85dd5690cad2d0838318ae22b7a5
Author: knighthat <git@knighthat.me>
Date:   2025-07-17

    shorten name

 .../kotlin/me/knighthat/innertube/response/NextResponse.kt |  8 ++++----
 .../me/knighthat/internal/response/NextResponseImpl.kt     | 14 +++++++-------
 2 files changed, 11 insertions(+), 11 deletions(-)

commit a8d39ca75388ce50fa8c05f75225456031d56126
Author: knighthat <git@knighthat.me>
Date:   2025-07-17

    make NextResponse.PlayerOverlays.PlayerOverlayRenderer.BrowserMediaSession.BrowserMediaSessionRenderer.album nullable

 src/main/kotlin/me/knighthat/innertube/response/NextResponse.kt    | 2 +-
 src/main/kotlin/me/knighthat/internal/response/NextResponseImpl.kt | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)

commit 27acc8c288ce3f8f6769f838d8410b7233b32ded
Author: knighthat <git@knighthat.me>
Date:   2025-07-13

    promote dependencies to implementation
    
    Includes:
    - org.jetbrains.kotlinx:kotlinx-coroutines-core
    - io.ktor:ktor-serialization-kotlinx-json

 build.gradle.kts | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

commit 28bd35f82282ce2613e5c57ce85bed97dc85e4dc
Author: knighthat <git@knighthat.me>
Date:   2025-07-12

    make BrowseResponse.Header.
    MusicImmersiveHeaderRenderer.description nullable

 src/main/kotlin/me/knighthat/innertube/response/BrowseResponse.kt    | 2 +-
 src/main/kotlin/me/knighthat/internal/response/BrowseResponseImpl.kt | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)

commit ffad7b80b17011216e09a5e626d70d89ddc811c3
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    method to retrieve song's basic info & related songs

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 87 ++++++++++++++++++++++
 1 file changed, 87 insertions(+)

commit 2e249111ecbe500977f807e88dda9f79845e26fa
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    adjust InnertubeArtistImpl.from to parse non-music channels

 .../internal/model/InnertubeArtistImpl.kt          |   49 +-
 .../innertube/response/BrowseResponseTest.kt       |    1 +
 .../internal/model/InnertubeArtistImplTest.kt      |  168 +-
 .../browse/endpoint_artist_non_music_response.json | 5408 ++++++++++++++++++++
 4 files changed, 5568 insertions(+), 58 deletions(-)

commit 7360203f9a89674dab4927333f6fe2f9ea04dd75
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    add MusicVisualHeaderRenderer for non-music channels

 .../kotlin/me/knighthat/innertube/response/BrowseResponse.kt   |  8 ++++++++
 .../me/knighthat/internal/response/BrowseResponseImpl.kt       | 10 +++++++++-
 2 files changed, 17 insertions(+), 1 deletion(-)

commit df3fbf9b0d0bf0e8ab8e0766cfa2ac8a4beb4e4c
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    add ServiceTracking to response's Context

 .../innertube/response/InnertubeResponse.kt        |  6 ++++++
 .../internal/response/InnertubeResponseImpl.kt     | 25 ++++++++++++++++++----
 2 files changed, 27 insertions(+), 4 deletions(-)

commit b9115d3cd38973b80b71e717be9e0663028b565b
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    move parse from InnertubeArtistImpl to InnertubeArtistImpl.SectionImpl from

 .../internal/model/InnertubeArtistImpl.kt          | 94 +++++++++++-----------
 1 file changed, 49 insertions(+), 45 deletions(-)

commit dbc1985465c77e1f44904d8c2b9a37bac7732e45
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    make BrowseResponse.Header.MusicImmersiveHeaderRenderer.monthlyListenerCount nullable

 src/main/kotlin/me/knighthat/innertube/response/BrowseResponse.kt    | 2 +-
 src/main/kotlin/me/knighthat/internal/model/InnertubeArtistImpl.kt   | 2 +-
 src/main/kotlin/me/knighthat/internal/response/BrowseResponseImpl.kt | 2 +-
 3 files changed, 3 insertions(+), 3 deletions(-)

commit 0186ad8e1389bd5377ca670554772d4665a84733
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    add MusicQueueRenderer for next song info

 .../me/knighthat/innertube/response/MusicQueueRenderer.kt | 11 +++++++++++
 src/main/kotlin/me/knighthat/innertube/response/Tabs.kt   |  1 +
 .../knighthat/internal/response/MusicQueueRendererImpl.kt | 15 +++++++++++++++
 .../kotlin/me/knighthat/internal/response/TabsImpl.kt     |  3 ++-
 4 files changed, 29 insertions(+), 1 deletion(-)

commit c2c1456fa765a90039097fe503dffcf9f68c04f6
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    make PlaylistPanelRenderer.playlistId & PlaylistPanelRenderer.numItemsToShow nullable

 .../kotlin/me/knighthat/innertube/response/PlaylistPanelRenderer.kt   | 4 ++--
 .../me/knighthat/internal/response/PlaylistPanelRendererImpl.kt       | 4 ++--
 2 files changed, 4 insertions(+), 4 deletions(-)

commit c753918e13af51bdc88af9056d3806a80da9e0d1
Author: knighthat <git@knighthat.me>
Date:   2025-07-09

    adjust ytmBrowse to accept context's components to build

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 108 ++++++++-------------
 .../kotlin/me/knighthat/innertube/InnertubeTest.kt |  13 ++-
 2 files changed, 51 insertions(+), 70 deletions(-)

commit 4e8c3dfc415569fa4c32302bf160f9e95b48c718
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    revert! swap innertube-java with innertube-kotlin

 build.gradle.kts | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)

commit e03efa3b76efce57015191b16c4dfcbd49827fbe
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    fixup! adjust visibility

 src/main/kotlin/me/knighthat/innertube/Innertube.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 11cf0b7640218398beb69910c4b8c846ae5c8b02
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    fixup! add songs fetcher to album when parse from BrowseResponse

 .../kotlin/me/knighthat/internal/model/InnertubeAlbumImplTest.kt     | 5 +++--
 1 file changed, 3 insertions(+), 2 deletions(-)

commit 9fb963b434a88d1bea76d4cff4e130ecb7377233
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    add methods to get album and its songs

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 59 ++++++++++++++++++++++
 1 file changed, 59 insertions(+)

commit 62efbec3c4a9b7f2b105e7a13453eb656f3b45a4
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    add songs fetcher to album when parse from BrowseResponse

 .../knighthat/internal/model/InnertubeAlbumImpl.kt | 98 +++++++++++++---------
 1 file changed, 58 insertions(+), 40 deletions(-)

commit d127aebd8f23fd7298d9c64ba558502397b68166
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    adjust visibility

 src/main/kotlin/me/knighthat/innertube/Innertube.kt | 8 +++-----
 1 file changed, 3 insertions(+), 5 deletions(-)

commit 64ea9d8e901d0355501839b9867d65c094dc8b7d
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    make MusicPlaylistShelfRenderer.playlistId and MusicPlaylistShelfRenderer.targetId nullable

 .../me/knighthat/innertube/response/MusicPlaylistShelfRenderer.kt     | 4 ++--
 src/main/kotlin/me/knighthat/internal/model/InnertubePlaylistImpl.kt  | 2 +-
 .../me/knighthat/internal/response/MusicPlaylistShelfRendererImpl.kt  | 4 ++--
 3 files changed, 5 insertions(+), 5 deletions(-)

commit 3611558e538d7d3de242c3328784fb06402debe1
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    add kotlinx-coroutines dependency

 build.gradle.kts          | 1 +
 gradle/libs.versions.toml | 4 ++++
 2 files changed, 5 insertions(+)

commit 0465f0cd367c9e5712616ea64074788f857b063b
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    add urlCanonical, subtitle, songs, and sections to InnertubeAlbum

 .../me/knighthat/innertube/model/InnertubeAlbum.kt |    32 +-
 .../knighthat/internal/model/InnertubeAlbumImpl.kt |   132 +-
 .../internal/model/InnertubeAlbumImplTest.kt       |   129 +-
 .../ytm/browse/album_browseResponse1.json          | 18385 +++++++++++++++++++
 .../ytm/browse/album_browseResponse2.json          | 12280 +++++++++++++
 5 files changed, 30949 insertions(+), 9 deletions(-)

commit 27f621096084ce8823459a0dd6720013f826ca3b
Author: knighthat <git@knighthat.me>
Date:   2025-07-08

    change DSL pageType form Runs.Run to Endpoint

 .../kotlin/me/knighthat/internal/model/InnertubeSongImpl.kt |  6 +++---
 src/main/kotlin/me/knighthat/internal/model/ItemUtils.kt    | 13 +++++++------
 2 files changed, 10 insertions(+), 9 deletions(-)

commit fec22adc09a49e27d843e14ee737c404c877564d
Author: knighthat <git@knighthat.me>
Date:   2025-07-07

    make MusicShelfRenderer.title nullable

 src/main/kotlin/me/knighthat/innertube/response/MusicShelfRenderer.kt   | 2 +-
 src/main/kotlin/me/knighthat/internal/model/InnertubeArtistImpl.kt      | 2 +-
 .../kotlin/me/knighthat/internal/response/MusicShelfRendererImpl.kt     | 2 +-
 3 files changed, 3 insertions(+), 3 deletions(-)

commit 2a1c11fd526a272589d472dbb7f2a911d78b5ad3
Author: knighthat <git@knighthat.me>
Date:   2025-07-07

    fixup! rename VideoViewCountRenderer to Renderer & merge MusicTastebuilderShelfThumbnailRenderer to Thumbnail.Renderer

 .../kotlin/me/knighthat/internal/response/SectionListRendererImpl.kt    | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit d7842ef1bb79a9207410d3da4151bf8bc0410b8a
Author: knighthat <git@knighthat.me>
Date:   2025-07-07

    add browse endpoint to retrieve artist

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 23 ++++++++++++++++++++++
 1 file changed, 23 insertions(+)

commit 83b8e605e78b1d5aac0d17ff5b0ab466560e40d2
Author: knighthat <git@knighthat.me>
Date:   2025-07-07

    turn Provider into functional interface

 src/main/kotlin/me/knighthat/innertube/Innertube.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit d71bd236f632a23a2f5ab3bbbf2f6151c7f4f99e
Author: knighthat <git@knighthat.me>
Date:   2025-07-07

    replace LinkedHashSet.removeFirst() with custom Pair for backward compatibility

 .../me/knighthat/internal/model/InnertubeAlbumImpl.kt   | 15 ++++++---------
 .../me/knighthat/internal/model/InnertubeSongImpl.kt    |  6 +++---
 .../kotlin/me/knighthat/internal/model/ItemUtils.kt     | 17 ++++++++++-------
 3 files changed, 19 insertions(+), 19 deletions(-)

commit 8f3c5f9601a74e38b4ff6105c30859659db69163
Author: knighthat <git@knighthat.me>
Date:   2025-07-07

    change BrowseResponse.contents to nullable

 .../kotlin/me/knighthat/innertube/Innertube.kt     |     3 +-
 .../knighthat/innertube/response/BrowseResponse.kt |     2 +-
 .../internal/model/InnertubeArtistImpl.kt          |     2 +-
 .../internal/response/BrowseResponseImpl.kt        |     3 +-
 .../innertube/response/BrowseResponseTest.kt       |     3 +-
 .../browse/playlist_continued_browseResponse.json  | 54829 +++++++++++++++++++
 6 files changed, 54835 insertions(+), 7 deletions(-)

commit 0ae0c2f96b6d31fe57ed5ae4cc0ccff41a35c2d0
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    implement AccessibleViaUrl for sharable urls

 .../kotlin/me/knighthat/innertube/model/AccessibleViaUrl.kt  | 12 ++++++++++++
 .../kotlin/me/knighthat/innertube/model/InnertubeArtist.kt   |  3 +--
 .../kotlin/me/knighthat/innertube/model/InnertubePlaylist.kt |  2 +-
 .../kotlin/me/knighthat/innertube/model/InnertubeSong.kt     |  2 +-
 .../me/knighthat/internal/model/InnertubeArtistImpl.kt       | 12 +++++++++++-
 .../me/knighthat/internal/model/InnertubePlaylistImpl.kt     |  9 ++++++---
 .../kotlin/me/knighthat/internal/model/InnertubeSongImpl.kt  |  6 ++++++
 src/main/kotlin/me/knighthat/internal/model/ItemUtils.kt     |  8 +++++---
 8 files changed, 43 insertions(+), 11 deletions(-)

commit d081bdad87cb2e96aa623ed619008cb2e466833c
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    remove unused code

 src/main/kotlin/me/knighthat/internal/model/InnertubeSongImpl.kt | 4 ----
 1 file changed, 4 deletions(-)

commit 351066c301e7b9322c11d309534717d04c5984b0
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    fixup! change durationText to nullable & replace word authors with artists

 src/main/kotlin/me/knighthat/internal/model/InnertubeSongImpl.kt | 8 +++-----
 1 file changed, 3 insertions(+), 5 deletions(-)

commit e94ab7221c9e5242bfede87dc61d6731d7dd048b
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    fix typo in doc

 src/main/kotlin/me/knighthat/innertube/model/InnertubeItem.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit e56a74d6a01e3d3577d74d9797784bf9416861ad
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    remove nullable element from httpMethod

 src/main/kotlin/me/knighthat/innertube/request/Request.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit c8a3295fe5976e905f9db8e5f530dab05c67db4b
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    swap innertube-java with innertube-kotlin

 build.gradle.kts | 3 +--
 1 file changed, 1 insertion(+), 2 deletions(-)

commit 12197ce6b68d1435146026cdce325e02ba71b11b
Author: Knight Hat <68310158+knighthat@users.noreply.github.com>
Date:   2025-07-07

    remove dependency-submission

 .github/workflows/test-on-push.yml | 20 +-------------------
 1 file changed, 1 insertion(+), 19 deletions(-)

commit ee2e982280aae96b902cd3e10a8d41cfdcf224e3
Author: Knight Hat <68310158+knighthat@users.noreply.github.com>
Date:   2025-07-07

    Create test-on-push.yml

 .github/workflows/test-on-push.yml | 56 ++++++++++++++++++++++++++++++++++++++
 1 file changed, 56 insertions(+)

commit 5da33875cfb9314fff82c580d5780bcaa47399b7
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    simple request template to Innertube and tests

 .../kotlin/me/knighthat/innertube/Innertube.kt     | 118 +++++++++++++++++++++
 .../kotlin/me/knighthat/innertube/InnertubeTest.kt | 115 ++++++++++++++++++++
 2 files changed, 233 insertions(+)

commit 7008edbf271ecc5f2726b8fa8123f29aa5197cd4
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    port Request to Kotlin

 .../me/knighthat/innertube/request/Request.java    | 34 ----------------------
 .../me/knighthat/innertube/request/Request.kt      | 34 ++++++++++++++++++++++
 2 files changed, 34 insertions(+), 34 deletions(-)

commit b7fe120be04f6b6fe16ce84baacfa4daf997b408
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    turn into sealed interface and @Serializable

 src/main/kotlin/me/knighthat/innertube/request/body/RequestBody.kt | 5 ++++-
 1 file changed, 4 insertions(+), 1 deletion(-)

commit 826872f4325473d32475f3e30c776fcb7a37bea5
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    remove duplicate repositories & fix spacing

 build.gradle.kts | 8 ++------
 1 file changed, 2 insertions(+), 6 deletions(-)

commit 9750a4c877a9f14af95681ed2757b314f654b6ab
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    add backup to description extractor

 .../me/knighthat/internal/model/InnertubeArtistImpl.kt   | 16 ++++++++--------
 1 file changed, 8 insertions(+), 8 deletions(-)

commit b885076a6d979bdf7b2b220f6a2406a735eb2c47
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    use correct implementation instead of interface

 .../kotlin/me/knighthat/internal/model/InnertubeArtistImplTest.kt | 8 ++++----
 1 file changed, 4 insertions(+), 4 deletions(-)

commit f7d9170785dcfbb9ac58c2d498ce11766794be38
Author: knighthat <git@knighthat.me>
Date:   2025-07-06

    fix typo

 src/test/resources/ytm/browse/artist_browseResponse.json | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit bf9b790ea345485658ce17a818d1b6370888e218
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    implement parsers & port tests of InnertubeSongImpl

 .../knighthat/internal/model/InnertubeSongImpl.kt  | 143 ++++-
 .../internal/model/InnertubeSongImplTest.kt        | 178 ++++++
 ...album_song_musicResponsiveListItemRenderer.json | 616 +++++++++++++++++++++
 ...rtist_song_musicResponsiveListItemRenderer.json | 568 +++++++++++++++++++
 .../artist_video_musicTwoRowItemRenderer.json      | 452 +++++++++++++++
 ...inued_song_musicResponsiveListItemRenderer.json | 610 ++++++++++++++++++++
 ...ist_song_musicResponsiveListItemRenderer_1.json | 530 ++++++++++++++++++
 ...ist_song_musicResponsiveListItemRenderer_2.json | 124 +++++
 .../ytm/next/song_playlistPanelVideoRenderer.json  | 611 ++++++++++++++++++++
 .../song_musicResponsiveListItemRenderer.json      | 548 ++++++++++++++++++
 10 files changed, 4379 insertions(+), 1 deletion(-)

commit 73257b7358dfedb00ad74447be79d56ae695b7d6
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    change durationText to nullable & replace word authors with artists

 src/main/kotlin/me/knighthat/innertube/model/InnertubeSong.kt    | 6 +++---
 src/main/kotlin/me/knighthat/internal/model/InnertubeSongImpl.kt | 6 +++---
 2 files changed, 6 insertions(+), 6 deletions(-)

commit 6069b5e47d519ea4bd3d6a19ec6cfc13f810e7aa
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    implement parsers & port tests of InnertubePlaylistImpl

 .../internal/model/InnertubePlaylistImpl.kt        |    50 +
 .../internal/model/InnertubePlaylistImplTest.kt    |   155 +
 .../artist_playlist_musicTwoRowItemRenderer.json   |   432 +
 .../playlist_twoColumnBrowseResultsRenderer1.json  | 55489 ++++++++++++++++++
 .../playlist_twoColumnBrowseResultsRenderer2.json  | 56932 +++++++++++++++++++
 .../playlist_twoColumnBrowseResultsRenderer3.json  | 44497 +++++++++++++++
 6 files changed, 157555 insertions(+)

commit 51905980fece14620b60e12c7821253e742166df
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    implement parsers & port tests of InnertubeArtistImpl

 .../internal/model/InnertubeArtistImpl.kt          |   125 +
 .../internal/model/InnertubeArtistImplTest.kt      |    81 +
 .../ytm/browse/artist_browseResponse.json          | 23934 +++++++++++++++++++
 ...st_related_artists_musicTwoRowItemRenderer.json |   207 +
 4 files changed, 24347 insertions(+)

commit 8fc4a0e8f4596676d70537769a6a720398d20ce9
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    implement parsers & port tests of InnertubeAlbumImpl

 .../knighthat/internal/model/InnertubeAlbumImpl.kt |  49 +-
 .../internal/model/InnertubeAlbumImplTest.kt       | 117 +++++
 .../album_alternative_musicTwoRowItemRenderer.json | 470 +++++++++++++++++++
 .../artist_album_musicTwoRowItemRenderer.json      | 442 ++++++++++++++++++
 .../music_home_album_musicTwoRowItemRenderer.json  | 485 +++++++++++++++++++
 .../album_musicResponsiveListItemRenderer.json     | 519 +++++++++++++++++++++
 6 files changed, 2081 insertions(+), 1 deletion(-)

commit 29862fa195913a5c4cdab890fd0033c35e3c930f
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    implement parsers & port tests of ContinuedPlaylistImpl

 .../internal/model/ContinuedPlaylistImpl.kt        |    25 +-
 .../me/knighthat/internal/model/ItemUtils.kt       |    52 +
 .../internal/model/ContinuedPlaylistImplTest.kt    |    56 +
 ...inued_musicPlaylistShelfRenderer_content_1.json | 60608 +++++++++++++++++++
 ...inued_musicPlaylistShelfRenderer_content_2.json | 59878 ++++++++++++++++++
 5 files changed, 120618 insertions(+), 1 deletion(-)

commit 6178ac4bd8b7cb547692b77dd19c00eec154d893
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    add subtitleText converter

 src/main/kotlin/me/knighthat/innertube/model/InnertubePlaylist.kt    | 2 ++
 src/main/kotlin/me/knighthat/internal/model/InnertubePlaylistImpl.kt | 5 ++++-
 2 files changed, 6 insertions(+), 1 deletion(-)

commit 03cb41e0d437f6f8ab4fb11618ffb99fc80e1351
Author: knighthat <git@knighthat.me>
Date:   2025-07-05

    change from var to val

 .../kotlin/me/knighthat/innertube/model/InnertubePlaylist.kt   | 10 +++++-----
 1 file changed, 5 insertions(+), 5 deletions(-)

commit af14d4575a41024e670d4e3eef85f3c792baa568
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    convert Section to interface

 .../me/knighthat/innertube/model/InnertubeArtist.kt      | 16 ++++++++++------
 .../me/knighthat/internal/model/InnertubeArtistImpl.kt   | 11 ++++++++++-
 2 files changed, 20 insertions(+), 7 deletions(-)

commit 6bfbfc818be9bc0f3515788822b0926f29ca592b
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    fix reference

 src/main/kotlin/me/knighthat/innertube/response/SectionListRenderer.kt | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 1421f4e2f99382ff16ccb309a2400f0ad2705bce
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    add subscription button to BrowseResponse for artist

 .../knighthat/innertube/response/BrowseResponse.kt |  1 +
 .../me/knighthat/innertube/response/Button.kt      | 25 +++++++++++++++++
 .../internal/response/BrowseResponseImpl.kt        |  4 ++-
 .../me/knighthat/internal/response/ButtonImpl.kt   | 31 ++++++++++++++++++++++
 4 files changed, 60 insertions(+), 1 deletion(-)

commit e3657df8713a6731a5874fbe35d0357d3774247b
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    replace Endpoint.Browse with Runs.Run of artists

 src/main/kotlin/me/knighthat/innertube/model/InnertubeAlbum.kt    | 4 ++--
 src/main/kotlin/me/knighthat/internal/model/InnertubeAlbumImpl.kt | 4 ++--
 2 files changed, 4 insertions(+), 4 deletions(-)

commit 38aa47a2f63ba4405d24642599135d5ee18dbd7a
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    implementations of models

 .../internal/model/ContinuedPlaylistImpl.kt        | 12 ++++++++++
 .../knighthat/internal/model/InnertubeAlbumImpl.kt | 16 +++++++++++++
 .../internal/model/InnertubeArtistImpl.kt          | 17 ++++++++++++++
 .../internal/model/InnertubePlaylistImpl.kt        | 26 ++++++++++++++++++++++
 .../knighthat/internal/model/InnertubeSongImpl.kt  | 24 ++++++++++++++++++++
 5 files changed, 95 insertions(+)

commit a249a475381c7f2e0489d8dfc5ffd9ecdb515224
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    introduce interface for innertube's components

 .../java/me/knighthat/innertube/Localized.java     | 16 +++++++
 .../me/knighthat/innertube/model/ContentRating.kt  | 11 +++++
 .../knighthat/innertube/model/ContinuedPlaylist.kt | 17 ++++++++
 .../me/knighthat/innertube/model/Descriptive.kt    | 12 ++++++
 .../me/knighthat/innertube/model/InnertubeAlbum.kt | 20 +++++++++
 .../knighthat/innertube/model/InnertubeArtist.kt   | 49 ++++++++++++++++++++++
 .../me/knighthat/innertube/model/InnertubeItem.kt  | 23 ++++++++++
 .../knighthat/innertube/model/InnertubePlaylist.kt | 26 ++++++++++++
 .../me/knighthat/innertube/model/InnertubeSong.kt  | 26 ++++++++++++
 9 files changed, 200 insertions(+)

commit 4df215ed41bb45d1865787469536c25296c6242d
Author: knighthat <git@knighthat.me>
Date:   2025-07-04

    add missing icon interface

 src/main/kotlin/me/knighthat/innertube/response/Badge.kt    |  6 ++++++
 src/main/kotlin/me/knighthat/internal/response/BadgeImpl.kt | 11 +++++++++--
 2 files changed, 15 insertions(+), 2 deletions(-)

commit 99c9d496b20e9ecd26759da02a8ececbdb423094
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    implement tests for requests body builders

 src/test/kotlin/me/knighthat/innertube/Utils.kt    |  1 +
 .../innertube/request/body/BrowseBodyTest.kt       | 46 ++++++++++++++++
 .../innertube/request/body/NextBodyTest.kt         | 52 ++++++++++++++++++
 .../innertube/request/body/PlayerBodyTest.kt       | 61 ++++++++++++++++++++++
 .../innertube/request/body/SearchBodyTest.kt       | 30 +++++++++++
 .../request/body/SearchSuggestionsBodyTest.kt      | 28 ++++++++++
 6 files changed, 218 insertions(+)

commit 6c4776b8a7efbe7e52a579815f816218acb3a48a
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    port request bodies to Kotlin

 .../me/knighthat/innertube/request/Request.java    | 34 +++++++++++
 .../innertube/request/body/BrowseBodyBuilder.java  | 47 ++++++++++++++++
 .../knighthat/innertube/request/body/Builder.java  | 21 +++++++
 .../innertube/request/body/NextBodyBuilder.java    | 47 ++++++++++++++++
 .../innertube/request/body/PlayerBodyBuilder.java  | 65 ++++++++++++++++++++++
 .../innertube/request/body/SearchBodyBuilder.java  | 40 +++++++++++++
 .../request/body/SearchSuggestionsBodyBuilder.java | 32 +++++++++++
 .../innertube/request/body/browse/TypeBuilder.java | 26 +++++++++
 .../innertube/request/body/next/Builder.java       | 30 ++++++++++
 .../innertube/request/body/player/Builder.java     | 46 +++++++++++++++
 .../innertube/request/body/search/Builder.java     | 36 ++++++++++++
 .../request/body/search/suggestions/Builder.java   | 14 +++++
 .../knighthat/innertube/request/body/BrowseBody.kt | 18 ++++++
 .../knighthat/innertube/request/body/NextBody.kt   | 18 ++++++
 .../knighthat/innertube/request/body/PlayerBody.kt | 24 ++++++++
 .../innertube/request/body/RequestBody.kt          |  6 ++
 .../knighthat/innertube/request/body/SearchBody.kt | 21 +++++++
 .../request/body/SearchSuggestionsBody.kt          | 17 ++++++
 18 files changed, 542 insertions(+)

commit 1f1de97f6b734fecdf279b814868af3b48065425
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    port localization and context

 .../knighthat/innertube/request/Localization.java  |  23 +++
 .../me/knighthat/innertube/request/body/Context.kt | 157 +++++++++++++++++++++
 2 files changed, 180 insertions(+)

commit 8aa6ef70de5d2caecb8d01424ae887756ac787bd
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    add org.junit.jupiter:junit-jupiter-params

 build.gradle.kts                                   |  4 +-
 gradle/libs.versions.toml                          |  1 +
 src/test/kotlin/me/knighthat/innertube/Utils.kt    | 14 +++++
 .../innertube/response/BrowseResponseTest.kt       | 64 ++++------------------
 .../me/knighthat/innertube/response/JsonParser.kt  | 11 ----
 .../innertube/response/NextResponseTest.kt         | 24 +++-----
 .../innertube/response/PlayerResponseTest.kt       | 26 +++------
 .../innertube/response/SearchResponseTest.kt       | 21 +++----
 .../response/SearchSuggestionsResponseTest.kt      | 22 +++-----
 9 files changed, 56 insertions(+), 131 deletions(-)

commit 78fd6a1f2ae5a664fb2c8f1f008e7be538f53ccc
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    update nullability

 .../response/MusicResponsiveListItemRenderer.kt    |  2 +-
 .../knighthat/innertube/response/PlayerResponse.kt | 43 ++++++++++++++++++----
 .../innertube/response/PlaylistPanelRenderer.kt    |  2 +-
 .../kotlin/me/knighthat/innertube/response/Tabs.kt |  2 +-
 .../response/MusicCarouselShelfRendererImpl.kt     |  4 +-
 .../MusicResponsiveListItemRendererImpl.kt         |  2 +-
 .../internal/response/PlayerResponseImpl.kt        | 15 ++++----
 .../internal/response/PlaylistPanelRendererImpl.kt | 32 +++++++++-------
 .../me/knighthat/internal/response/TabsImpl.kt     |  2 +-
 9 files changed, 68 insertions(+), 36 deletions(-)

commit 16805164ca11c8c3305cd57d18446e6d316a6d29
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    fixup! port implementations of response components

 .../innertube/response/MusicCarouselShelfRenderer.kt  |  2 +-
 .../knighthat/internal/response/BrowseResponseImpl.kt |  6 +++---
 .../response/MusicCarouselShelfRendererImpl.kt        | 10 +++++-----
 .../response/MusicPlaylistShelfRendererImpl.kt        | 19 +++++++++----------
 .../knighthat/internal/response/PlayerResponseImpl.kt |  8 ++++----
 .../internal/response/SectionListRendererImpl.kt      | 18 ++++++++----------
 6 files changed, 30 insertions(+), 33 deletions(-)

commit 497ca30ff29e56239de569aa1f465ff6ed5fa716
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    basic files

 .../java/me/knighthat/innertube/Constants.java     | 33 ++++++++++++++++++++++
 .../java/me/knighthat/innertube/Endpoints.java     | 24 ++++++++++++++++
 src/main/java/me/knighthat/innertube/PageType.java | 15 ++++++++++
 .../java/me/knighthat/innertube/SearchFilter.java  | 25 ++++++++++++++++
 .../java/me/knighthat/innertube/UserAgents.java    | 16 +++++++++++
 5 files changed, 113 insertions(+)

commit 116fa163f15c496105c23baa48225d6b02530a70
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    add basic response tests

 .../innertube/response/BrowseResponseTest.kt       |    70 +
 .../me/knighthat/innertube/response/JsonParser.kt  |    11 +
 .../innertube/response/NextResponseTest.kt         |    33 +
 .../innertube/response/PlayerResponseTest.kt       |    35 +
 .../innertube/response/SearchResponseTest.kt       |    25 +-
 .../response/SearchSuggestionsResponseTest.kt      |    33 +
 .../ytm/browse/endpoint_artist_response.json       | 28202 +++++++++++++++++++
 .../ytm/browse/endpoint_playlist_response.json     | 17477 ++++++++++++
 src/test/resources/ytm/next/endpoint_response.json |  1253 +
 .../resources/ytm/player/endpoint_response.json    |  1023 +
 .../resources/ytm/search/endpoint_response.json    | 10379 +++++++
 .../ytm/search_suggestions/endpoint_response.json  |  1821 ++
 12 files changed, 60345 insertions(+), 17 deletions(-)

commit 0e9eb83cc4e8678ce73840302d22c2d03ee6e125
Author: knighthat <git@knighthat.me>
Date:   2025-07-02

    rename VideoViewCountRenderer to Renderer & merge MusicTastebuilderShelfThumbnailRenderer to Thumbnail.Renderer

 .../kotlin/me/knighthat/innertube/response/PrimaryResults.kt  |  4 ++--
 .../me/knighthat/innertube/response/SectionListRenderer.kt    |  7 +------
 .../me/knighthat/internal/response/PrimaryResultsImpl.kt      |  6 +++---
 .../me/knighthat/internal/response/SectionListRendererImpl.kt | 11 ++---------
 4 files changed, 8 insertions(+), 20 deletions(-)

commit 2864cf2e1aa6c320c15ef8220185fb0e70aec9be
Author: knighthat <git@knighthat.me>
Date:   2025-07-01

    port implementations of response components

 .../internal/response/AccessibilityImpl.kt         |  15 +++
 .../me/knighthat/internal/response/BadgeImpl.kt    |  18 ++++
 .../internal/response/BrowseResponseImpl.kt        |  68 +++++++++++++
 .../internal/response/ContinuationImpl.kt          |  16 +++
 .../me/knighthat/internal/response/EndpointImpl.kt | 109 +++++++++++++++++++++
 .../internal/response/InnertubeResponseImpl.kt     |  15 +++
 .../knighthat/internal/response/MicroformatImpl.kt |  51 ++++++++++
 .../response/MusicCardShelfRendererImpl.kt         |  18 ++++
 .../response/MusicCarouselShelfRendererImpl.kt     |  33 +++++++
 .../response/MusicPlaylistShelfRendererImpl.kt     |  41 ++++++++
 .../MusicResponsiveListItemRendererImpl.kt         |  39 ++++++++
 .../internal/response/MusicShelfRendererImpl.kt    |  19 ++++
 .../response/MusicTwoRowItemRendererImpl.kt        |  15 +++
 .../internal/response/NextResponseImpl.kt          |  61 ++++++++++++
 .../me/knighthat/internal/response/OverlayImpl.kt  |  29 ++++++
 .../internal/response/PlayerResponseImpl.kt        | 101 +++++++++++++++++++
 .../internal/response/PlaylistPanelRendererImpl.kt |  32 ++++++
 .../internal/response/PrimaryResultsImpl.kt        |  88 +++++++++++++++++
 .../me/knighthat/internal/response/RunsImpl.kt     |  18 ++++
 .../internal/response/SearchResponseImpl.kt        |  16 +++
 .../response/SearchSuggestionsResponseImpl.kt      |  36 +++++++
 .../internal/response/SectionListRendererImpl.kt   |  87 ++++++++++++++++
 .../knighthat/internal/response/SimpleTextImpl.kt  |  10 ++
 .../me/knighthat/internal/response/TabsImpl.kt     |  31 ++++++
 .../knighthat/internal/response/ThumbnailImpl.kt   |  17 ++++
 .../knighthat/internal/response/ThumbnailsImpl.kt  |  17 ++++
 .../innertube/response/SearchResponseTest.kt       |  39 ++++++++
 27 files changed, 1039 insertions(+)

commit 380e6f45fc90040eff604fc498c41c268deff5d0
Author: knighthat <git@knighthat.me>
Date:   2025-07-01

    port response components from java to kotlin

 .../knighthat/innertube/response/Accessibility.kt  | 15 ++++
 .../me/knighthat/innertube/response/Badge.kt       | 19 +++++
 .../knighthat/innertube/response/BrowseResponse.kt | 60 +++++++++++++
 .../knighthat/innertube/response/Continuation.kt   | 13 +++
 .../me/knighthat/innertube/response/Endpoint.kt    | 97 ++++++++++++++++++++++
 .../innertube/response/InnertubeResponse.kt        | 24 ++++++
 .../me/knighthat/innertube/response/Microformat.kt | 47 +++++++++++
 .../innertube/response/MusicCardShelfRenderer.kt   | 15 ++++
 .../response/MusicCarouselShelfRenderer.kt         | 29 +++++++
 .../response/MusicPlaylistShelfRenderer.kt         | 34 ++++++++
 .../response/MusicResponsiveListItemRenderer.kt    | 35 ++++++++
 .../innertube/response/MusicShelfRenderer.kt       | 16 ++++
 .../innertube/response/MusicTwoRowItemRenderer.kt  | 13 +++
 .../knighthat/innertube/response/NextResponse.kt   | 52 ++++++++++++
 .../me/knighthat/innertube/response/Overlay.kt     | 24 ++++++
 .../knighthat/innertube/response/PlayerResponse.kt | 92 ++++++++++++++++++++
 .../innertube/response/PlaylistPanelRenderer.kt    | 32 +++++++
 .../knighthat/innertube/response/PrimaryResults.kt | 78 +++++++++++++++++
 .../me/knighthat/innertube/response/Response.kt    | 10 +++
 .../kotlin/me/knighthat/innertube/response/Runs.kt | 15 ++++
 .../knighthat/innertube/response/SearchResponse.kt | 12 +++
 .../response/SearchSuggestionsResponse.kt          | 29 +++++++
 .../innertube/response/SectionListRenderer.kt      | 76 +++++++++++++++++
 .../me/knighthat/innertube/response/SimpleText.kt  |  8 ++
 .../kotlin/me/knighthat/innertube/response/Tabs.kt | 26 ++++++
 .../me/knighthat/innertube/response/Thumbnail.kt   | 14 ++++
 .../me/knighthat/innertube/response/Thumbnails.kt  | 14 ++++
 27 files changed, 899 insertions(+)

commit 8bb7fb5da34dd230d1512b120d75adadb4123539
Author: knighthat <git@knighthat.me>
Date:   2025-07-01

    add kotlin sdk

 build.gradle.kts          | 15 ++++-----------
 gradle/libs.versions.toml |  8 +++++++-
 2 files changed, 11 insertions(+), 12 deletions(-)

commit 8a9a8ef64700dd8fbc05dacff4c664f570ce7719
Author: knighthat <git@knighthat.me>
Date:   2025-07-01

    init commit

 .gitignore                               | 14 ++++----------
 .idea/codeStyles/codeStyleConfig.xml     |  2 +-
 .idea/dictionaries/project.xml           |  9 +++++++++
 .idea/gradle.xml                         |  1 +
 .idea/kotlinc.xml                        |  2 +-
 build.gradle.kts                         |  9 ++++++++-
 gradle/wrapper/gradle-wrapper.properties |  3 ++-
 7 files changed, 26 insertions(+), 14 deletions(-)

commit 3eb1fb90c47ac18081440d5776a918a7df88d67e
Author: knighthat <git@knighthat.me>
Date:   2025-07-01

    init commit

 .gitignore | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)

commit 1a66afd040c210c91cdfd973fa1b1f5d2df68f11
Author: knighthat <git@knighthat.me>
Date:   2025-07-01

    init commit

 .gitignore                               |  45 +++
 .idea/.gitignore                         |   5 +
 .idea/codeStyles/Project.xml             | 598 +++++++++++++++++++++++++++++++
 .idea/codeStyles/codeStyleConfig.xml     |   5 +
 .idea/gradle.xml                         |  16 +
 .idea/kotlinc.xml                        |   6 +
 .idea/misc.xml                           |   7 +
 .idea/vcs.xml                            |   6 +
 build.gradle.kts                         |  41 +++
 gradle.properties                        |   1 +
 gradle/libs.versions.toml                |  23 ++
 gradle/wrapper/gradle-wrapper.properties |   6 +
 gradlew                                  | 234 ++++++++++++
 gradlew.bat                              |  89 +++++
 settings.gradle.kts                      |   1 +
 15 files changed, 1083 insertions(+)

```

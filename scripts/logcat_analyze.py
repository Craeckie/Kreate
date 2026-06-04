#!/usr/bin/env python3
"""
Parse a Kreate logcat file and summarise playback resolution events.

Extracts:
  - Which InnerTube client was tried for each video
  - Whether each attempt succeeded or failed (and why)
  - HTTP-level errors (400, 403, etc.) from the player endpoint
  - CipherDeobfuscator / PoTokenGenerator failures
  - ExoPlayer playback errors

Usage:
  python3 scripts/logcat_analyze.py <logcat.txt>
  python3 scripts/logcat_analyze.py <logcat.txt> --video <videoId>
  adb logcat | python3 scripts/logcat_analyze.py -          # live stream

Pass --verbose to show every matched log line instead of just the summary.
"""
import re
import sys
import argparse
from collections import defaultdict

# ── Patterns ────────────────────────────────────────────────────────────────

RE_TS = re.compile(r"(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d+)")
# YouTube video IDs are exactly 11 chars of [A-Za-z0-9_-]; used to pull the id
# out of a matched line's capture groups (client names / ranges / reasons won't match).
RE_VID = re.compile(r"^[A-Za-z0-9_-]{11}$")

PATTERNS = [
    # YTPlayerUtils info/debug (metrolist path — kept for compatibility with older builds)
    ("main_client_fail",   re.compile(r"Main client .+ failed completely for videoId=(\S+)")),
    ("client_try",         re.compile(r"Trying (?:stream from MAIN_CLIENT|fallback client \d+/\d+): (.+)")),
    ("client_status_ok",   re.compile(r"Player response status OK for client: (.+)")),
    ("client_status_bad",  re.compile(r"Player response status not OK: (.+)")),
    ("stream_ok",          re.compile(r"Playback: client=(\S+), videoId=(\S+)")),
    ("stream_validated",   re.compile(r"Stream validated successfully with client: (.+)")),
    ("stream_validate_fail", re.compile(r"Stream validation failed for client: (.+)")),
    ("format_none",        re.compile(r"No suitable format found for client: (.+)")),
    ("all_failed",         re.compile(r"Bad stream player response - all clients failed")),
    # dataspec resolver path (AndroidVrStreamHelper / InnertubeResolvingDataSource)
    ("vr_resolved",        re.compile(r"Resolved (\S+) via (\S+)")),
    ("vr_method",          re.compile(r"Getting online stream url for \"(\S+)\" with method (\S+)")),
    ("unavailable",        re.compile(r"Playability status not OK.*This video is unavailable")),
    # current fallback/validation/rematch flow (InnertubeResolvingDataSource + StatefulPlayerImpl)
    ("vr_fallback",        re.compile(r"ANDROID_VR failed for (\S+) \(([^)]+)\); falling back to IOS")),
    ("range_rejected",     re.compile(r"Stream url range (\S+) rejected: HTTP (\d+)")),
    ("marking_unplayable", re.compile(r"(\S+) stream url for (\S+) failed validation")),
    ("unplayable_exc",     re.compile(r"UnplayableException: (.+)")),
    ("rematch_attempt",    re.compile(r"Song unplayable \(([^)]+)\); attempting rematch for (\S+)")),
    ("rematch_search",     re.compile(r"Rematch(?:.*)?: searching replacement for (\S+) \((.+)\)")),
    ("rematch_strong",     re.compile(r"Rematch STRONG: swapping (\S+) (?:→|->) (\S+)")),
    ("rematch_weak",       re.compile(r"Rematch WEAK: asking user to confirm replacement for (\S+)")),
    ("rematch_none",       re.compile(r"Rematch: no candidates found for (\S+)")),
    ("got_403",            re.compile(r"Got HTTP 403 for (\S+); clearing")),
    ("http_403_persisted", re.compile(r"HTTP 403 for (\S+) persisted")),
    # Metrolist-path failure signatures (from logcat 13803 analysis)
    ("potoken_asset_miss", re.compile(r"FileNotFoundException: po_token\.html")),
    ("ios_override_403",   re.compile(r"<-- 403 https://\S+[?&]c=IOS")),
    # Signature / PO token
    ("sig_ts_ok",          re.compile(r"Signature timestamp obtained via NewPipe: (\d+)")),
    ("sig_ts_fail",        re.compile(r"Failed to get signature timestamp via NewPipe")),
    ("potoken_fail",       re.compile(r"PoToken generation failed: (.+)")),
    ("potoken_ok",         re.compile(r"PoToken generated successfully")),
    ("cipher_uninit",      re.compile(r"lateinit property appContext has not been initialized")),
    # HTTP errors at player endpoint
    ("http_400_player",    re.compile(r"<-- 400 https://(?:music\.youtube\.com|www\.youtube\.com)/(?:player|embed)")),
    ("http_403",           re.compile(r"<-- 403 https://")),
    ("http_400_iframe",    re.compile(r"<-- 400 https://www\.youtube\.com/iframe_api")),
    # ExoPlayer / Ktor
    ("exo_error",          re.compile(r"ExoPlaybackException|Playback error")),
    ("ktor_400",           re.compile(r"ClientRequestException.+invalid: 400")),
    ("exception",          re.compile(r"\[PLAYBACK_DEBUG\] EXCEPTION during playback for videoId=(\S+): (.+)")),
    ("success_debug",      re.compile(r"\[PLAYBACK_DEBUG\] SUCCESS: Got playback data")),
    # Song-info (metadata) parse failures — songBasicInfo / upsertSongInfo path
    ("songinfo_upsert_fail", re.compile(r"failed to upsert (\S+?)'?s? information to database")),
    ("missing_field",      re.compile(r"MissingFieldException: Field '(\S+)' is required")),
    # Thumbnail / image loading
    ("image_null",         re.compile(r"RealImageLoader:.*Unable to create a fetcher that supports: null")),
    ("thumb_oversized",    re.compile(r"=w\d+-h\d+\S*-w900-h900")),
    # Benign device-capability noise
    ("reverb_fail",        re.compile(r"Reverb init failed|AudioEffect.*initCheck failed")),
    # UI crashes
    ("changelog_crash",    re.compile(r"ChangelogsDialog.*(?:IndexOutOfBounds|IndexOutOfBoundsException)")),
]


def ts_of(line):
    m = RE_TS.search(line)
    return m.group(1) if m else "??"


def vid_of(groups):
    """Pick the YouTube video id out of a pattern's capture groups, if present."""
    for g in groups:
        if g and RE_VID.match(g):
            return g
    return None


def analyze(lines, filter_video=None, verbose=False):
    events = []  # (timestamp, tag, data)

    for line in lines:
        for tag, pat in PATTERNS:
            m = pat.search(line)
            if m:
                events.append((ts_of(line), tag, m.groups(), line.rstrip()))
                break  # one match per line is enough

    if not events:
        print("No playback events found. Is the log from a Kreate build?")
        return

    # ── Summary ──────────────────────────────────────────────────────────────
    counts = defaultdict(int)
    for _, tag, _, _ in events:
        counts[tag] += 1

    print("=" * 70)
    print("PLAYBACK EVENT SUMMARY")
    print("=" * 70)

    issues = []

    if counts["cipher_uninit"]:
        issues.append(f"  [CRITICAL] CipherDeobfuscator.appContext not initialized "
                      f"({counts['cipher_uninit']}x) — CipherDeobfuscator.initialize(context) "
                      f"missing from MainApplication.onCreate()")

    if counts["potoken_asset_miss"]:
        issues.append(f"  [ERROR] po_token.html not found in APK assets ({counts['potoken_asset_miss']}x) "
                      f"— file is in res/raw/ but not assets/; PoToken can never be generated for web clients")

    if counts["ios_override_403"]:
        issues.append(f"  [ERROR] NewPipe IOS-override 403 ({counts['ios_override_403']}x) "
                      f"— c=IOS stream URLs 403 (IOS needs a GVS PO token); "
                      f"pot-free ANDROID_VR streams being overwritten")

    if counts["unavailable"]:
        issues.append(f"  [ERROR] 'This video is unavailable' ({counts['unavailable']}x) "
                      f"— all clients exhausted (metrolist YTPlayerUtils path failure)")

    if counts["songinfo_upsert_fail"]:
        fields = sorted({g[0] for _, t, g, _ in events if t == "missing_field"})
        if fields:
            field_str = ", ".join(fields)
            issues.append(f"  [ERROR] songBasicInfo parse failed (missing field {field_str}) "
                          f"({counts['songinfo_upsert_fail']}x) — a now-required-by-our-schema "
                          f"field is absent from YouTube's response; song still plays but metadata "
                          f"never lands in the DB and a 'couldn't fetch song info' toast shows")
        else:
            issues.append(f"  [ERROR] songBasicInfo / upsertSongInfo failed "
                          f"({counts['songinfo_upsert_fail']}x) — song metadata not written to DB")

    if counts["image_null"]:
        issues.append(f"  [WARN] image request with null model ({counts['image_null']}x) "
                      f"— ImageFactory built a request for a null thumbnail URL (usually benign: "
                      f"artwork requested before a song loads)")

    if counts["thumb_oversized"]:
        issues.append(f"  [INFO] {counts['thumb_oversized']} oversized/chained thumbnail requests "
                      f"— size params stacked (e.g. =w60-h60…-w900-h900); small rows decoding 900px "
                      f"JPEGs. ThumbnailSizeInterceptor should be rewriting these")

    if counts["reverb_fail"]:
        issues.append(f"  [INFO] audio effect unavailable on device ({counts['reverb_fail']}x) "
                      f"— Reverb/AudioEffect init failed; benign, device lacks the effect")

    if counts["changelog_crash"]:
        issues.append(f"  [ERROR] changelog dialog crash ({counts['changelog_crash']}x) "
                      f"— ChangelogsDialog index out of bounds")

    if counts["http_400_iframe"]:
        issues.append(f"  [WARN] YouTube /iframe_api returned 400 ({counts['http_400_iframe']}x) "
                      f"— NewPipe embed-page fetch blocked (IP/rate-limit)")

    if counts["http_400_player"]:
        issues.append(f"  [WARN] music.youtube.com/player returned 400 ({counts['http_400_player']}x) "
                      f"— WEB_REMIX blocked; must fall through to ANDROID_VR clients")

    if counts["main_client_fail"]:
        issues.append(f"  [INFO] Main client (WEB_REMIX) failed {counts['main_client_fail']}x "
                      f"— fallback clients should kick in")

    if counts["potoken_fail"]:
        issues.append(f"  [WARN] PO-token generation failed {counts['potoken_fail']}x — "
                      f"WEB_REMIX/WEB streams will likely 403 at ~1 min mark")

    if counts["sig_ts_fail"] and not counts["sig_ts_ok"]:
        issues.append(f"  [WARN] Signature timestamp fetch failed {counts['sig_ts_fail']}x "
                      f"— n-transform and WEB client cipher may not work")

    if counts["all_failed"]:
        issues.append(f"  [ERROR] All clients exhausted without a working stream "
                      f"({counts['all_failed']}x) — check which VR client version YouTube accepts")

    if counts["http_403"]:
        issues.append(f"  [WARN] 403 on stream URL ({counts['http_403']}x) — "
                      f"likely missing PO token on WEB/IOS client")

    if counts["vr_fallback"]:
        issues.append(f"  [INFO] ANDROID_VR fell back to IOS {counts['vr_fallback']}x "
                      f"— VR returned UNPLAYABLE/empty; IOS attempted next")

    if counts["marking_unplayable"]:
        vids = sorted({g[1] for _, t, g, _ in events if t == "marking_unplayable" and len(g) > 1})
        issues.append(f"  [WARN] IOS stream teaser-blocked → marked unplayable "
                      f"({counts['marking_unplayable']}x){' for ' + ', '.join(vids) if vids else ''} "
                      f"— VR-unplayable + IOS pot-blocked; routed to auto-rematch")

    # Rematch ping-pong: A→B and B→A both present means two dead uploads swap forever.
    swaps = [(g[0], g[1]) for _, t, g, _ in events if t == "rematch_strong" and len(g) > 1]
    pingpong = sorted({frozenset(p) for p in swaps if (p[1], p[0]) in swaps})
    if pingpong:
        pairs = "; ".join(" ↔ ".join(sorted(p)) for p in pingpong)
        issues.append(f"  [ERROR] Rematch PING-PONG ({pairs}) — STRONG rematch keeps swapping "
                      f"between equally-unplayable uploads of the same song; song never plays")

    if counts["rematch_weak"]:
        vids = sorted({g[0] for _, t, g, _ in events if t == "rematch_weak"})
        issues.append(f"  [INFO] Rematch asked user to confirm (WEAK) for {', '.join(vids)}")

    if counts["http_403_persisted"]:
        issues.append(f"  [ERROR] 403 persisted after re-resolution {counts['http_403_persisted']}x "
                      f"— stuck song (older-build signature; current build marks unplayable instead)")

    if counts["vr_resolved"]:
        vr_wins = [g for _, t, g, _ in events if t == "vr_resolved"]
        client_wins = defaultdict(int)
        for g in vr_wins:
            client_wins[g[1]] += 1
        summary = ", ".join(f"{c}×{n}" for c, n in client_wins.items())
        issues.append(f"  [OK]  Resolved via dataspec path: {summary}")

    if counts["stream_ok"]:
        winners = [g for _, t, g, _ in events if t == "stream_ok"]
        client_wins = defaultdict(int)
        for g in winners:
            client_wins[g[0]] += 1
        summary = ", ".join(f"{c}×{n}" for c, n in client_wins.items())
        issues.append(f"  [OK]  Successful playback resolutions (metrolist): {summary}")

    if not issues:
        issues.append("  No significant issues detected.")

    for i in issues:
        print(i)

    print()

    # ── Timeline ─────────────────────────────────────────────────────────────
    print("=" * 70)
    print("TIMELINE (key events)")
    print("=" * 70)

    SHOW_TAGS = {
        "main_client_fail", "client_status_ok", "client_status_bad",
        "stream_ok", "stream_validate_fail", "all_failed",
        "sig_ts_fail", "potoken_fail", "cipher_uninit",
        "http_400_player", "http_400_iframe", "http_403",
        "exo_error", "ktor_400", "exception", "success_debug",
        # dataspec / VR resolver path
        "vr_resolved", "unavailable", "potoken_asset_miss", "ios_override_403",
        # current fallback / validation / rematch flow
        "vr_fallback", "range_rejected", "marking_unplayable", "unplayable_exc",
        "rematch_attempt", "rematch_search", "rematch_strong", "rematch_weak",
        "rematch_none", "got_403", "http_403_persisted",
        # song-info parse + image loading + UI
        "songinfo_upsert_fail", "missing_field", "image_null",
        "thumb_oversized", "changelog_crash",
    }

    shown = 0
    current_vid = None  # carried forward so id-less lines (e.g. EXO ERROR) get attributed
    for ts, tag, groups, raw in events:
        v = vid_of(groups)
        if v:
            current_vid = v
        if verbose or tag in SHOW_TAGS:
            label = tag.upper().replace("_", " ")
            detail = " | ".join(str(g) for g in groups) if groups else ""
            vid = v or (f"~{current_vid}" if current_vid else "?")  # ~ = inferred from context
            print(f"  {ts}  [{vid:12}] [{label}]  {detail}")
            shown += 1

    if shown == 0:
        print("  (no key events — try --verbose)")

    print()
    print(f"Total events matched: {len(events)}")


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("logfile", help="logcat file path, or - for stdin")
    ap.add_argument("--video", help="filter events by videoId substring")
    ap.add_argument("--verbose", action="store_true",
                    help="print every matched log line")
    args = ap.parse_args()

    if args.logfile == "-":
        lines = sys.stdin
    else:
        with open(args.logfile, encoding="utf-8", errors="replace") as f:
            lines = f.readlines()

    if args.video:
        lines = [l for l in lines if args.video in l]

    analyze(lines, filter_video=args.video, verbose=args.verbose)


if __name__ == "__main__":
    main()

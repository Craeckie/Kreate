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

PATTERNS = [
    # YTPlayerUtils info/debug
    ("main_client_fail",   re.compile(r"Main client .+ failed completely for videoId=(\S+)")),
    ("client_try",         re.compile(r"Trying (?:stream from MAIN_CLIENT|fallback client \d+/\d+): (.+)")),
    ("client_status_ok",   re.compile(r"Player response status OK for client: (.+)")),
    ("client_status_bad",  re.compile(r"Player response status not OK: (.+)")),
    ("stream_ok",          re.compile(r"Playback: client=(\S+), videoId=(\S+)")),
    ("stream_validated",   re.compile(r"Stream validated successfully with client: (.+)")),
    ("stream_validate_fail", re.compile(r"Stream validation failed for client: (.+)")),
    ("format_none",        re.compile(r"No suitable format found for client: (.+)")),
    ("all_failed",         re.compile(r"Bad stream player response - all clients failed")),
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
]


def ts_of(line):
    m = RE_TS.search(line)
    return m.group(1) if m else "??"


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

    if counts["stream_ok"]:
        winners = [g for _, t, g, _ in events if t == "stream_ok"]
        client_wins = defaultdict(int)
        for g in winners:
            client_wins[g[0]] += 1
        summary = ", ".join(f"{c}×{n}" for c, n in client_wins.items())
        issues.append(f"  [OK]  Successful playback resolutions: {summary}")

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
    }

    shown = 0
    for ts, tag, groups, raw in events:
        if verbose or tag in SHOW_TAGS:
            label = tag.upper().replace("_", " ")
            detail = " | ".join(str(g) for g in groups) if groups else ""
            print(f"  {ts}  [{label}]  {detail}")
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

#!/usr/bin/env python3
"""
Validate the ANDROID_VR resolution path AND diagnose the "starts, then stops
near ~1 min" symptom, without an Android build.

Replicates the InnerTube player request AndroidVrStreamHelper.kt builds
(mutated ofAndroidClient -> ANDROID_VR 1.65.10) and compares with the legacy IOS
path. Two failure modes are checked, both bounded so the run is quick:

  1. Late-range 403  -> server serves the first ~minute then rejects later byte
     ranges (needs a PO token).  Emulated with real HTTP Range headers, the way
     ExoPlayer/OkHttp request chunks.
  2. n-throttling     -> first buffer is fast, the rest crawls below real-time so
     playback stalls once the buffer drains.  Sampled for a few seconds only.

Usage:  python3 vr_probe.py [videoId ...] [--client VR|IOS|both]
"""
import json
import sys
import time
import urllib.request
from urllib.parse import urlparse, parse_qs

YOUTUBEI = "https://www.youtube.com/youtubei/v1/"
GAPIS = "https://youtubei.googleapis.com/youtubei/v1/"
HTTP_TIMEOUT = 20

CLIENTS = {
    "VR": {
        "host": YOUTUBEI,
        "client": {
            "clientName": "ANDROID_VR", "clientVersion": "1.65.10",
            "platform": "MOBILE", "deviceMake": "Oculus", "deviceModel": "Quest 3",
            "osName": "Android", "osVersion": "12L", "androidSdkVersion": 32,
            "hl": "en", "gl": "US", "utcOffsetMinutes": 0,
        },
        "ua": "com.google.android.apps.youtube.vr.oculus/1.65.10 "
              "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
        "cn": "28", "cv": "1.65.10",
    },
    "IOS": {
        "host": GAPIS,
        "client": {
            "clientName": "IOS", "clientVersion": "21.03.2",
            "platform": "MOBILE", "deviceMake": "Apple", "deviceModel": "iPhone16,2",
            "osName": "iOS", "osVersion": "18.7.2.22H124",
            "hl": "en", "gl": "US", "utcOffsetMinutes": 0,
        },
        "ua": "com.google.ios.youtube/21.03.2 (iPhone16,2; U; CPU iOS 18_7_2 like Mac OS X;)",
        "cn": "5", "cv": "21.03.2",
    },
}


def _headers(cfg):
    return {"User-Agent": cfg["ua"], "X-Goog-Api-Format-Version": "2",
            "X-YouTube-Client-Name": cfg["cn"], "X-YouTube-Client-Version": cfg["cv"]}


def _post(url, cfg, body):
    req = urllib.request.Request(
        url, data=json.dumps(body).encode(), method="POST",
        headers={"Content-Type": "application/json", **_headers(cfg)})
    with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT) as r:
        return json.loads(r.read().decode())


def _ctx(client):
    return {"context": {"client": client,
                        "request": {"internalExperimentFlags": [], "useSsl": True},
                        "user": {"lockedSafetyMode": False}}}


def resolve(cfg, video_id):
    """Return (playabilityStatus, audio_format_or_None)."""
    vd = _post(cfg["host"] + "visitor_id?prettyPrint=false", cfg, _ctx(cfg["client"])) \
        .get("responseContext", {}).get("visitorData")
    client = dict(cfg["client"])
    if vd:
        client["visitorData"] = vd
    body = {**_ctx(client), "videoId": video_id, "cpn": "abcdefghijkl",
            "contentCheckOk": True, "racyCheckOk": True}
    resp = _post(cfg["host"] + "player?prettyPrint=false", cfg, body)
    ps = resp.get("playabilityStatus", {})
    fmts = [f for f in resp.get("streamingData", {}).get("adaptiveFormats", [])
            if f.get("mimeType", "").startswith("audio")]
    fmts.sort(key=lambda f: f.get("bitrate", 0))
    return ps, (fmts[-1] if fmts else None)


def range_get(url, ua, start, length=524288):
    """One ranged GET using a real HTTP Range header (as ExoPlayer does)."""
    req = urllib.request.Request(url, headers={
        "User-Agent": ua, "Range": f"bytes={start}-{start + length - 1}"})
    try:
        with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT) as r:
            return r.status, len(r.read())
    except urllib.error.HTTPError as e:
        return e.code, 0
    except Exception as e:
        return f"ERR {type(e).__name__}", 0


def diagnose(name, video_id):
    cfg = CLIENTS[name]
    ps, fmt = resolve(cfg, video_id)
    print(f"  [{name}] playabilityStatus={ps.get('status')} reason={ps.get('reason')}")
    if ps.get("status") != "OK" or not fmt:
        return
    if "url" not in fmt:
        print(f"  [{name}] itag={fmt['itag']} url is ciphered (needs JS player)")
        return
    url, ua = fmt["url"], cfg["ua"]
    q = parse_qs(urlparse(url).query)
    clen = int(fmt.get("contentLength") or q.get("clen", [0])[0] or 0)
    br = fmt.get("bitrate", 0)
    bps = br / 8 if br else 0   # bytes of stream per second of audio
    total_s = clen / bps if bps else 0
    print(f"  [{name}] itag={fmt['itag']} {fmt['mimeType'].split(';')[0]} "
          f"clen={clen}B (~{total_s:.0f}s) n={q.get('n', ['<none>'])[0]}")

    # (1) Late-range probe: emulate sequential chunk reads at 0/30/60/90/120s + tail.
    print(f"  [{name}] ranged GETs (status,bytes):", end=" ")
    blocked_at = None
    for sec in (0, 30, 60, 90, 120):
        start = int(bps * sec)
        if clen and start >= clen:
            break
        st, n = range_get(url, ua, start)
        print(f"{sec}s={st}", end="  ")
        if st != 206 and st != 200 and blocked_at is None:
            blocked_at = sec
    if clen:
        st, n = range_get(url, ua, max(clen - 262144, 0), 262144)
        print(f"tail={st}", end="")
    print()
    if blocked_at is not None:
        print(f"  [{name}] *** LATE-RANGE BLOCK at ~{blocked_at}s -> would stop near there ***")

    # (2) Bounded throughput sample: read for up to SAMPLE_S wall-clock seconds.
    SAMPLE_S = 6
    req = urllib.request.Request(url, headers={"User-Agent": ua})
    t0 = time.time(); got = 0; code = None; err = None
    try:
        with urllib.request.urlopen(req, timeout=HTTP_TIMEOUT) as r:
            code = r.status
            while time.time() - t0 < SAMPLE_S:
                b = r.read(65536)
                if not b:
                    break
                got += len(b)
    except urllib.error.HTTPError as e:
        code, err = e.code, "HTTPError"
    except Exception as e:
        err = f"{type(e).__name__}"
    dt = time.time() - t0
    speed = got / dt / 1024 if dt else 0
    need = bps / 1024 if bps else 0
    print(f"  [{name}] throughput ~{speed:.0f} KB/s (need {need:.0f} KB/s real-time) "
          f"http={code}" + (f" err={err}" if err else ""))
    if not err and need and speed < need * 1.1:
        print(f"  [{name}] *** THROTTLED below real-time -> playback will stall ***")


def main():
    args = sys.argv[1:]
    which = "both"
    if "--client" in args:
        i = args.index("--client"); which = args[i + 1]; del args[i:i + 2]
    vids = args or ["dQw4w9WgXcQ"]
    names = {"VR": ["VR"], "IOS": ["IOS"], "both": ["VR", "IOS"]}[which]
    for vid in vids:
        print(f"\n=== {vid} ===")
        for name in names:
            try:
                diagnose(name, vid)
            except Exception as e:
                print(f"  [{name}] failed: {type(e).__name__}: {e}")


if __name__ == "__main__":
    main()

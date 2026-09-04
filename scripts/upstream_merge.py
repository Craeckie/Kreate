#!/usr/bin/env python3
"""Merge knighthat/Kreate into this fork without re-learning the same traps.

Cherry-pick triage does not scale here: of 113 outstanding upstream commits, ~85%
were Crowdin/dependabot churn and reading them one by one cost far more than a
merge did. A merge collapses the whole backlog and, crucially, RESETS THE MERGE
BASE, so the next run only has to reason about genuinely new work.

The conflicts are never the hard part -- git names them and you fix them. The
hard part is the file git merged *silently* because the two sides touched
different lines of a file this fork deliberately rewrote. That is how upstream's
swapped PO tokens, a Compose duplicate-key crash and a DELETE that wipes local
songs' playlist rows all sailed in clean on 2026-09-04.

So this script reports three buckets, in increasing order of danger:

  CONFLICT   git stopped; you already know to look.
  PROTECTED  upstream touched a path where our version always wins; auto-resolved.
  SILENT     both sides changed the file, git merged it clean, NOBODY LOOKED.
             Read every one of these before trusting the build.

Usage:
    python3 scripts/upstream_merge.py                 # fetch + dry-run report
    python3 scripts/upstream_merge.py --no-fetch      # offline
    python3 scripts/upstream_merge.py --merge         # merge + auto-resolve PROTECTED
    python3 scripts/upstream_merge.py --replay HEAD   # re-analyse an existing merge commit
"""
from __future__ import annotations

import argparse
import subprocess
import sys
import tempfile
from pathlib import Path

UPSTREAM = "upstream/main"

# Paths where OUR version wins, always. Each entry is (glob, reason).
# A merge must never quietly hand these back to upstream.
PROTECTED: list[tuple[str, str]] = [
    ("modules/innertube", "submodule pointer -- Craeckie fork, needs SSH to bump"),
    ("modules/metrolist", "submodule pointer -- Craeckie fork, needs SSH to bump"),
    ("composeApp/src/androidMain/kotlin/com/metrolist/music/utils/YTPlayerUtils.kt",
     "dead code here -- zero callers; our resolver is AndroidVrStreamHelper"),
    ("composeApp/src/androidMain/kotlin/com/metrolist/music/utils/cipher/",
     "serves only the dead YTPlayerUtils path"),
    ("composeApp/src/androidMain/kotlin/com/metrolist/music/utils/potoken/PoTokenGenerator.kt",
     "upstream swaps the two PO tokens; our live ANDROID_VR path needs the video-bound one first"),
    ("composeApp/src/androidMain/kotlin/app/kreate/di/InnertubeResolvingDataSource.kt",
     "our resolver is AndroidVrStreamHelper; upstream re-adds the YTPlayerUtils path"),
    ("composeApp/src/androidMain/assets/solver/",
     "~400KB of JS referenced by nothing once the cipher rewrite is kept out"),
    (".github/workflows/build-all-flavors-weekly.yml",
     "rewritten for this fork's signing secrets (KEYSTORE_BASE64/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD)"),
    (".github/workflows/build-nightly.yaml",
     "schedule removed -- this fork has none of the NIGHTLY_* secrets, so a scheduled run can only fail"),
]

# Paths that always need a human decision, even when git merges them clean.
# These are the subsystems where this fork's behaviour intentionally differs.
ALWAYS_REVIEW: list[tuple[str, str]] = [
    ("composeApp/src/commonMain/kotlin/app/kreate/database/", "schema/migrations -- data loss risk"),
    ("composeApp/src/commonMain/kotlin/app/kreate/di/DatabaseModule.kt",
     "upstream has shipped DELETEs here that strand local: songs"),
    ("composeApp/src/androidMain/kotlin/app/kreate/android/service/", "playback service"),
    ("composeApp/src/androidMain/kotlin/app/kreate/di/PlayerModule.kt", "ExoPlayer data source chain"),
    ("gradle/libs.versions.toml", "versionCode/versionName use our Debian-style scheme"),
]


def git(*args: str, cwd: str | None = None) -> str:
    return subprocess.run(
        ["git", *args], cwd=cwd, check=True, capture_output=True, text=True
    ).stdout.strip()


def git_ok(*args: str, cwd: str | None = None) -> bool:
    return subprocess.run(["git", *args], cwd=cwd, capture_output=True).returncode == 0


def matches(path: str, patterns: list[tuple[str, str]]) -> str | None:
    for pat, reason in patterns:
        if path == pat or (pat.endswith("/") and path.startswith(pat)):
            return reason
    return None


def changed(a: str, b: str) -> set[str]:
    out = git("diff", "--name-only", a, b)
    return {line for line in out.splitlines() if line}


def classify(base: str, ours: str, theirs: str) -> dict:
    theirs_files = changed(base, theirs)
    ours_files = changed(base, ours)
    overlap = sorted(theirs_files & ours_files)

    conflicts = trial_merge(ours, theirs)

    buckets = {"conflict": [], "protected": [], "silent": [], "review": []}
    for path in sorted(theirs_files):
        reason = matches(path, PROTECTED)
        if reason:
            buckets["protected"].append((path, reason))
            continue
        if path in conflicts:
            buckets["conflict"].append((path, "git could not merge"))
            continue
        reason = matches(path, ALWAYS_REVIEW)
        if reason:
            buckets["review"].append((path, reason))
        elif path in overlap:
            buckets["silent"].append((path, "both sides changed this file; merged clean"))
    return buckets


def trial_merge(ours: str, theirs: str) -> set[str]:
    """Merge in a throwaway worktree so the real tree is never touched."""
    with tempfile.TemporaryDirectory(prefix="kreate-trialmerge-") as tmp:
        wt = str(Path(tmp) / "wt")
        if not git_ok("worktree", "add", "--detach", wt, ours):
            return set()
        try:
            subprocess.run(
                ["git", "merge", "--no-commit", "--no-ff", theirs],
                cwd=wt, capture_output=True, text=True,
            )
            out = subprocess.run(
                ["git", "diff", "--name-only", "--diff-filter=U"],
                cwd=wt, capture_output=True, text=True,
            ).stdout
            return {line for line in out.splitlines() if line}
        finally:
            subprocess.run(["git", "worktree", "remove", "--force", wt], capture_output=True)
            subprocess.run(["git", "worktree", "prune"], capture_output=True)


def ledger_notes(base: str, theirs: str) -> list[str]:
    """Surface skipped/deferred verdicts that this merge would pull in anyway."""
    tsv = Path("docs/upstream-triage.tsv")
    if not tsv.exists():
        return []
    incoming = set()
    for line in git("log", "--format=%H", f"{base}..{theirs}").splitlines():
        incoming.add(line[:9])
    notes = []
    for row in tsv.read_text(encoding="utf-8").splitlines():
        parts = row.split("\t")
        if len(parts) < 3:
            continue
        sha, verdict, note = parts[0].strip(), parts[1].strip(), parts[2].strip()
        if verdict in ("skipped", "deferred") and sha[:9] in incoming:
            notes.append(f"{sha[:9]}  [{verdict}]  {note}")
    return notes


def section(title: str, rows: list[tuple[str, str]], blurb: str = "") -> None:
    print(f"\n## {title}  ({len(rows)})")
    if blurb:
        print(f"   {blurb}")
    for path, reason in rows:
        print(f"  {path}")
        print(f"      -> {reason}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--no-fetch", action="store_true", help="use refs already on disk")
    ap.add_argument("--merge", action="store_true", help="perform the merge and auto-resolve PROTECTED paths")
    ap.add_argument("--replay", metavar="MERGE_COMMIT",
                    help="re-analyse an existing merge commit (uses its two parents)")
    args = ap.parse_args()

    if args.replay:
        ours, theirs = f"{args.replay}^1", f"{args.replay}^2"
    else:
        if not args.no_fetch:
            subprocess.run(["git", "fetch", "upstream"], check=False)
        ours, theirs = "HEAD", UPSTREAM

    base = git("merge-base", ours, theirs)
    behind = git("rev-list", "--count", f"{ours}..{theirs}")
    print(f"merge base : {base[:9]}")
    print(f"{ours} .. {theirs}: {behind} commits")
    if behind == "0" and not args.replay:
        print("\nNothing to merge.")
        return 0

    buckets = classify(base, ours, theirs)
    section("CONFLICT -- git stopped here", buckets["conflict"])
    section("PROTECTED -- our version always wins", buckets["protected"],
            "--merge resolves these to ours automatically.")
    section("ALWAYS REVIEW -- sensitive subsystem", buckets["review"],
            "Merged clean or not, read the diff.")
    section("SILENT -- both sides touched it, git merged it clean", buckets["silent"],
            "THIS IS THE DANGEROUS BUCKET. Read every diff before trusting the build.")

    notes = ledger_notes(base, theirs)
    if notes:
        print(f"\n## LEDGER -- previously skipped/deferred, coming in anyway  ({len(notes)})")
        print("   A merge does not honour a cherry-pick verdict. Re-read these notes.")
        for n in notes:
            print(f"  {n}")

    if not args.merge:
        print("\n(dry run -- rerun with --merge to perform the merge)")
        return 0

    print(f"\n=== merging {theirs} ===")
    subprocess.run(["git", "merge", "--no-commit", "--no-ff", theirs], check=False)
    for path, reason in buckets["protected"]:
        if git_ok("checkout", "--ours", "--", path) or git_ok("checkout", "HEAD", "--", path):
            subprocess.run(["git", "add", "--", path], check=False)
            print(f"  resolved to ours: {path}")
    remaining = git("diff", "--name-only", "--diff-filter=U")
    print("\nStill unresolved:")
    print("  " + ("\n  ".join(remaining.splitlines()) if remaining else "(none)"))
    print("\nNext: resolve the above, then read every SILENT/REVIEW diff with")
    print("  git diff HEAD -- <path>")
    print("before committing. Then verify with a build.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

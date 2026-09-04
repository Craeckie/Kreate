#!/usr/bin/env python3
"""Triage upstream (knighthat/Kreate) commits we have not taken yet.

Answers, in one shot, the question this fork asks after every upstream push:
"is there anything worth cherry-picking, and will it apply?"

    python3 scripts/upstream_check.py              # fetch + full report
    python3 scripts/upstream_check.py --no-fetch   # use the refs already on disk
    python3 scripts/upstream_check.py --all        # include i18n/deps/ci noise
    python3 scripts/upstream_check.py --json       # machine-readable

For every commit in `main..upstream/main` it reports:

  category   i18n / deps / ci / version / submodule / docs / code
  state      NEW, or a verdict remembered from docs/upstream-triage.tsv
  apply      CLEAN, CONFLICT(files...), or SUBMODULE (needs a pointer bump we
             cannot do from a machine without push access to the submodule forks)

Commits whose patch is already in our history -- cherry-picked, or arrived via
the one upstream merge -- are detected with `git cherry` (patch-id equality)
plus the "cherry picked from commit <sha>" trailer that `git cherry-pick -x`
leaves behind, and are dropped from the report.

Record decisions in docs/upstream-triage.tsv so the next run stays short:

    <sha>\t<verdict>\t<note>          verdict: taken | skipped | deferred
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEDGER = os.path.join(REPO_ROOT, "docs", "upstream-triage.tsv")

# Paths that carry no engineering decision -- Crowdin churn and store metadata.
# Translations live in three places (android res, KMP composeResources, fastlane)
# and Crowdin also regenerates the credit JSONs, so match all of them.
I18N_PREFIXES = (
    "composeApp/src/androidMain/res/values-",
    "composeApp/src/commonMain/composeResources/values-",
    "fastlane/metadata/android/",
)
I18N_FILES = (
    "composeApp/src/androidMain/res/raw/translators.json",
    "composeApp/src/androidMain/res/raw/contributors.json",
)
# Our own release notes are not Crowdin output -- they are a real signal.
I18N_EXCEPT = "fastlane/metadata/android/en-US/changelogs/"


def git(*args: str, cwd: str = REPO_ROOT) -> str:
    return subprocess.run(
        ("git",) + args, cwd=cwd, capture_output=True, text=True, check=False
    ).stdout.strip()


def is_submodule_path(path: str) -> bool:
    return path in ("modules/innertube", "modules/metrolist", "modules/kizzy", "scripts/strings-modifier")


def classify(subject: str, author: str, files: list[str]) -> str:
    """Bucket a commit by what it actually changes, not by its message."""
    if not files:
        return "empty"
    if subject.startswith("Bump ") or "dependabot" in author:
        return "deps"

    def i18n(p: str) -> bool:
        if p in I18N_FILES:
            return True
        return p.startswith(I18N_PREFIXES) and not p.startswith(I18N_EXCEPT)

    if all(i18n(p) for p in files):
        return "i18n"
    if all(p.startswith(".github/") for p in files):
        return "ci"
    if all(is_submodule_path(p) or p == ".gitmodules" for p in files):
        return "submodule"
    if all(
        p.startswith(I18N_EXCEPT) or p == "gradle/libs.versions.toml" or i18n(p)
        for p in files
    ):
        return "version"
    if all(p.endswith((".md", ".txt")) or p.startswith("docs/") for p in files):
        return "docs"
    return "code"


def load_ledger() -> dict[str, tuple[str, str]]:
    out: dict[str, tuple[str, str]] = {}
    if not os.path.exists(LEDGER):
        return out
    with open(LEDGER) as fh:
        for line in fh:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            sha = parts[0].strip()
            verdict = parts[1].strip() if len(parts) > 1 else "?"
            note = parts[2].strip() if len(parts) > 2 else ""
            # Accept abbreviated SHAs in the ledger; key on the full one.
            full = git("rev-parse", "--verify", "--quiet", sha + "^{commit}") or sha
            out[full] = (verdict, note)
    return out


def already_applied(base: str, ours: str, theirs: str) -> set[str]:
    """Upstream SHAs whose patch is already in our history."""
    applied: set[str] = set()
    # Patch-id equality: `git cherry` prints '-' for equivalent commits.
    for line in git("cherry", ours, theirs, base).splitlines():
        if line.startswith("-"):
            applied.add(line.split()[1])
    # `git cherry-pick -x` trailers -- catches picks we reworded or squashed.
    for line in git("log", "--format=%H%n%B", f"{base}..{ours}").splitlines():
        if "cherry picked from commit" in line:
            applied.add(line.split("cherry picked from commit")[1].strip(" )."))
    return applied


def _reset(worktree: str) -> None:
    for cmd in (("git", "cherry-pick", "--abort"), ("git", "reset", "--hard", "-q"),
                ("git", "clean", "-fdq")):
        subprocess.run(cmd, cwd=worktree, capture_output=True, check=False)


def probe(worktree: str, sha: str, files: list[str]) -> str:
    """Test-apply a commit in a throwaway worktree; never touches the real tree.

    A commit that bumps a submodule pointer cannot be cherry-picked here -- the
    submodule forks are SSH-only and the referenced commit is usually not even in
    our fork of the submodule yet. For those we apply only the superproject paths,
    which answers the question that actually matters: "is the app-side hunk
    self-contained enough to take on its own?"
    """
    plain = [p for p in files if not is_submodule_path(p) and p != ".gitmodules"]
    has_sub = len(plain) != len(files)

    if not plain:
        return "SUBMODULE-ONLY"

    if has_sub:
        diff = subprocess.run(
            ("git", "diff", f"{sha}^", sha, "--") + tuple(plain),
            cwd=worktree, capture_output=True, text=True, check=False,
        ).stdout
        res = subprocess.run(
            ("git", "apply", "--3way", "--index", "-"),
            cwd=worktree, input=diff, capture_output=True, text=True, check=False,
        )
        prefix = "SUBMODULE+"
    else:
        res = subprocess.run(
            ("git", "cherry-pick", "--no-commit", sha),
            cwd=worktree, capture_output=True, text=True, check=False,
        )
        prefix = ""

    if res.returncode == 0:
        verdict = prefix + "CLEAN"
    else:
        conflicted = git("diff", "--name-only", "--diff-filter=U", cwd=worktree).splitlines()
        names = ", ".join(os.path.basename(p) for p in conflicted) or "apply failed"
        verdict = prefix + "CONFLICT(" + names + ")"
    _reset(worktree)
    return verdict


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--no-fetch", action="store_true", help="skip `git fetch upstream`")
    ap.add_argument("--all", action="store_true",
                    help="include i18n / deps / ci commits (hidden by default)")
    ap.add_argument("--no-probe", action="store_true",
                    help="skip the cherry-pick dry run (much faster)")
    ap.add_argument("--json", action="store_true", help="emit JSON instead of a table")
    ap.add_argument("--ours", default="main")
    ap.add_argument("--theirs", default="upstream/main")
    args = ap.parse_args()

    if not args.no_fetch:
        # --no-recurse-submodules: the submodule forks are SSH-only, and a missing
        # key turns a routine fetch into a wall of "Permission denied (publickey)".
        subprocess.run(
            ("git", "fetch", "upstream", "--tags", "--no-recurse-submodules"),
            cwd=REPO_ROOT, check=False,
        )

    base = git("merge-base", args.ours, args.theirs)
    if not base:
        print(f"error: no merge base between {args.ours} and {args.theirs}", file=sys.stderr)
        return 1

    applied = already_applied(base, args.ours, args.theirs)
    ledger = load_ledger()

    shas = git("rev-list", "--reverse", f"{args.ours}..{args.theirs}").split()
    records = []
    for sha in shas:
        if sha in applied or sha[:12] in applied:
            continue
        subject = git("log", "-1", "--format=%s", sha)
        author = git("log", "-1", "--format=%an <%ae>", sha)
        date = git("log", "-1", "--format=%ad", "--date=short", sha)
        files = git("show", "--pretty=", "--name-only", sha).splitlines()
        verdict, note = ledger.get(sha, ("NEW", ""))
        records.append({
            "sha": sha[:9], "full_sha": sha, "subject": subject, "author": author,
            "date": date, "files": files, "category": classify(subject, author, files),
            "state": verdict, "note": note, "apply": "?",
        })

    decided = {"taken", "skipped"}
    interesting = [
        r for r in records
        if r["category"] in ("code", "version", "docs", "submodule")
        and r["state"] not in decided
    ]
    to_probe = [r for r in (records if args.all else interesting) if r["state"] == "NEW"]

    if to_probe and not args.no_probe:
        tmp = tempfile.mkdtemp(prefix="upstream-probe-")
        wt = os.path.join(tmp, "wt")
        subprocess.run(("git", "worktree", "add", "--detach", wt, args.ours),
                       cwd=REPO_ROOT, capture_output=True, check=False)
        try:
            for r in to_probe:
                r["apply"] = probe(wt, r["full_sha"], r["files"])
        finally:
            subprocess.run(("git", "worktree", "remove", "--force", wt),
                           cwd=REPO_ROOT, capture_output=True, check=False)
            shutil.rmtree(tmp, ignore_errors=True)

    if args.json:
        print(json.dumps(records, indent=2))
        return 0

    counts: dict[str, int] = {}
    for r in records:
        counts[r["category"]] = counts.get(r["category"], 0) + 1
    print(f"merge base : {base[:9]}  ({git('log', '-1', '--format=%s', base)[:60]})")
    ruled_on = sum(1 for r in records if r["state"] in decided)
    print(f"{args.ours} .. {args.theirs}: {len(shas)} commits, "
          f"{len(shas) - len(records)} already applied, "
          f"{ruled_on} ruled on in the ledger, "
          f"{len(records) - ruled_on} outstanding")
    print("by category: " + ", ".join(f"{k}={v}" for k, v in sorted(counts.items())))
    print()

    shown = records if args.all else interesting
    if not shown:
        print("Nothing outstanding worth a look.")
        return 0

    for cat in ("code", "submodule", "version", "docs", "i18n", "deps", "ci", "empty"):
        rows = [r for r in shown if r["category"] == cat]
        if not rows:
            continue
        print(f"## {cat}  ({len(rows)})")
        for r in rows:
            flag = r["state"] if r["state"] != "NEW" else r["apply"]
            print(f"  {r['sha']}  {r['date']}  [{flag}]  {r['subject'][:72]}")
            if r["note"]:
                print(f"             note: {r['note']}")
        print()

    if not args.all:
        hidden = len([r for r in records if r["category"] in ("i18n", "deps", "ci", "empty")])
        if hidden:
            print(f"({hidden} i18n/deps/ci commits hidden -- rerun with --all to see them.)")
    print("Record decisions in docs/upstream-triage.tsv to keep future runs short.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

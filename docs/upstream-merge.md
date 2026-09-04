# Merging upstream (knighthat/Kreate)

## Merge, don't cherry-pick

The 2026-07 batch was triaged commit by commit: 113 outstanding commits, of which ~85% were
Crowdin and dependabot churn. That triage cost hours and still left five commits parked as
`deferred`. Merging the same 113 commits produced **six conflicts**, four of them one-liners.

A merge also **resets the merge base**, so the next run reasons only about genuinely new work.
Cherry-picking never moves the base, so every future run re-walks the same backlog.

Use `docs/upstream-triage.tsv` as a *notebook*, not a gate: a merge does not honour a
cherry-pick verdict, so anything previously marked `skipped`/`deferred` comes in anyway unless
you actively resolve it back out.

## The workflow

```bash
python3 scripts/upstream_merge.py            # fetch + dry-run report
python3 scripts/upstream_merge.py --merge    # merge, auto-resolving PROTECTED paths to ours
```

Then, before committing:

```bash
git diff HEAD -- <path>     # for EVERY path in the SILENT and ALWAYS REVIEW buckets
```

and verify with a real build (see CLAUDE.md — release-type, no R8, no lint on this box).

`--replay <merge-commit>` re-runs the analysis against an existing merge, which is how you check
the policy lists still describe reality.

## The three buckets, in increasing order of danger

| Bucket | Meaning |
|---|---|
| `CONFLICT` | git stopped. You already know to look. Historically the *easiest* part. |
| `PROTECTED` | Upstream touched a path where our version always wins. Auto-resolved to ours. |
| `SILENT` | **Both sides changed the file and git merged it clean.** Nobody looks here by default. |

`SILENT` is the bucket that matters. On 2026-09-04 it hid three real defects that a clean
`git merge` reported as success:

- **Swapped PO tokens.** Upstream reordered `PoTokenResult(playerRequestPoToken, streamingDataPoToken)`.
  Harmless for upstream, whose live path is `YTPlayerUtils`. Our live path
  (`InnertubeResolvingDataSource` → `AndroidVrStreamHelper`) sends `playerRequestPoToken` as the
  ANDROID_VR `/player` token, which must be the **video-id-bound** one. The swap would have sent
  the session-bound token and broken playback.
- **A Compose duplicate-key crash.** The Home ViewModel refactor concatenates the YouTube sync
  result with the Room query and feeds it to `items( key = Album::id )`. An album in both lists
  crashes with "Key was already used".
- **Silent data loss.** A new `DatabaseInitCallback` runs
  `DELETE FROM song_playlist_map WHERE song_id NOT IN (SELECT id FROM songs)` on every DB open.
  `From35To36Migration` strips the `local:` prefix from `songs.id` **without** updating the
  referencing tables, so every local file's playlist membership, lyrics, formats and playback
  history would be deleted on the next app open.

None of these conflicted. All three were in files where this fork had deliberately diverged.

## Standing constraints

- **Submodule pointers cannot be bumped here.** `modules/innertube` and `modules/metrolist` point
  at `git@github.com:Craeckie/…` forks; our innertube fork is a squashed orphan sharing no history
  with upstream's. There is no SSH key on this box. Always resolve these to ours; a change that
  genuinely needs a new submodule commit is a task for a machine with push access.
- **`origin` is an SSH remote and pushing over it fails here.** `gh` is authenticated over HTTPS:
  ```bash
  git push "https://x-access-token:$(gh auth token)@github.com/Craeckie/Kreate.git" main --tags
  ```
- **Much of upstream's playback work lands on code we do not execute.**
  `com.metrolist.music.utils.YTPlayerUtils` and `utils/cipher/` have **zero callers** here — our
  resolver is `InnertubeResolvingDataSource` → `AndroidVrStreamHelper`. Taking upstream's rewrite
  of them buys nothing at runtime and drags in ~400KB of assets. Keep ours; check for a live
  caller before ever taking a "fix" there.
- **The version block is always ours** — Debian-style `<upstream>-<N>`, see CLAUDE.md.

## Keeping the policy honest

`PROTECTED` and `ALWAYS_REVIEW` live at the top of `scripts/upstream_merge.py`, each entry with a
reason. When a subsystem stops being dead, or a new one diverges, edit those lists — that is the
whole maintenance burden. Re-run `--replay HEAD` after a merge to confirm the lists still match
what actually happened.

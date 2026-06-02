#!/usr/bin/env python3
"""PostToolUse hook: auto-commit Kreate repo changes after every Edit/Write."""
import json, sys, subprocess, os

d = json.load(sys.stdin)
f = d.get('tool_input', {}).get('file_path', '')
repo = '/workspace/Kreate'

if not f.startswith(repo + '/'):
    sys.exit(0)

# Stage the specific edited file plus any other tracked modifications,
# but do NOT add untracked files (avoids sweeping in stray artifacts).
subprocess.run(['git', '-C', repo, 'add', '-u'], capture_output=True)
if os.path.exists(f):
    subprocess.run(['git', '-C', repo, 'add', f], capture_output=True)

if subprocess.run(['git', '-C', repo, 'diff', '--cached', '--quiet'], capture_output=True).returncode == 0:
    sys.exit(0)

rel = os.path.relpath(f, repo)
subprocess.run(['git', '-C', repo, 'commit', '-m', f'wip: {rel}'])

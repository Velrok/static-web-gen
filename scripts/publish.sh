#!/bin/bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
targetdir="$repo_root/../velrok.github.io"

[[ -d "$targetdir/.git" ]] || { echo "Error: $targetdir is not a git repo" >&2; exit 1; }

cd "$repo_root"
lein gen

rsync -a --delete --exclude='.git' public/ "$targetdir/"

src_sha=$(git rev-parse --short HEAD)
cd "$targetdir"
git add .
if git diff --cached --quiet; then
  echo "No changes to publish."
else
  git commit -m "publish from $src_sha"
  git push
fi

echo DONE

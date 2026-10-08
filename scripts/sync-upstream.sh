#!/usr/bin/env bash
# Upstream releases pair a client commit with an extractor commit in their wrapper repo
# (InfinityLoop1308/PipePipe). Merge exactly that pair so the composite build stays compatible.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ -n "$(git status --porcelain)" ]]; then
    echo "working tree not clean" >&2
    exit 1
fi

client_sha=$(gh api repos/InfinityLoop1308/PipePipe/contents/PipePipeClient --jq .sha)
extractor_sha=$(gh api repos/InfinityLoop1308/PipePipe/contents/PipePipeExtractor --jq .sha)

git fetch upstream
git merge --no-edit "$client_sha"

git -C PipePipeExtractor fetch origin
git -C PipePipeExtractor checkout --quiet "$extractor_sha"
git add PipePipeExtractor
if ! git diff --cached --quiet; then
    git commit -m "bump extractor to ${extractor_sha:0:7}"
fi

echo "client at ${client_sha:0:7}, extractor at ${extractor_sha:0:7}"

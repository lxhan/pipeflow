#!/usr/bin/env bash
# Publishes the arm64 release APK as a GitHub release for Obtainium. Tags are
# v<upstream version>-pf<n>, n counting PipeFlow releases on that upstream version; the APK's
# versionName carries the same suffix so Obtainium can match installed and released versions.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ -n "$(git status --porcelain)" ]]; then
    echo "working tree not clean" >&2
    exit 1
fi
if [[ "$(git rev-parse --abbrev-ref HEAD)" != "main" ]]; then
    echo "not on main" >&2
    exit 1
fi
git fetch --quiet origin main --tags
if [[ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]]; then
    echo "main differs from origin/main, push first" >&2
    exit 1
fi

version=$(sed -n 's/^def appVersionName = "\(.*\)"$/\1/p' app/build.gradle)
last=$(git tag -l "v${version}-pf*" | sed "s/^v${version}-pf//" | sort -n | tail -1)
release="pf$(( ${last:-0} + 1 ))"
tag="v${version}-${release}"

previous=$(git tag -l "v*-pf*" --sort=-creatordate | head -1)
if [[ -n "$previous" ]]; then
    notes=$(git log --format="- %s" "${previous}..HEAD")
else
    notes="First PipeFlow release, based on PipePipe ${version}."
fi

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
KEY_PATH="$HOME/.android/debug.keystore" KEY_STORE_PASSWORD=android \
    KEY_ALIAS=androiddebugkey KEY_PASSWORD=android \
    ./gradlew :app:assembleRelease -PpipeflowRelease="$release"

apk="app/build/outputs/apk/release/PipeFlow_${version}-${release}-arm64-v8a-release.apk"
gh release create "$tag" "$apk" --repo lxhan/pipeflow --target "$(git rev-parse HEAD)" \
    --title "$tag" --notes "$notes"
git fetch --quiet origin --tags

echo "released $tag"

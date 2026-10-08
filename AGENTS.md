# PipeFlow

Personal fork of PipePipe's client (`InfinityLoop1308/PipePipeClient`). Design: `docs/superpowers/specs/2026-10-08-pipeflow-design.md`.

## Layout

- App code lives here. The Java/Kotlin namespace stays `org.schabi.newpipe` to keep upstream merges small.
- `PipePipeExtractor/` is upstream's extractor as a submodule, consumed through `includeBuild`. Never edit it; changes belong in the client.
- `main` = an upstream release commit plus fork commits. Sync with `scripts/sync-upstream.sh`, not GitHub's "Sync fork" (that tracks upstream `dev`).

## Scripts

Every gradle command needs:

    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    export ANDROID_HOME="$HOME/Library/Android/sdk"

- Build debug: `./gradlew :app:assembleDebug`
- Build release: `KEY_PATH="$HOME/.android/pipeflow.jks" KEY_STORE_PASSWORD=... KEY_ALIAS=pipeflow KEY_PASSWORD=... ./gradlew :app:assembleRelease`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- Lint (report only, never fails the build): `./gradlew :app:lintDebug`
- Install on phone: `adb install -r app/build/outputs/apk/release/PipeFlow_*-arm64-v8a-release.apk`
- Sync upstream release: `scripts/sync-upstream.sh`

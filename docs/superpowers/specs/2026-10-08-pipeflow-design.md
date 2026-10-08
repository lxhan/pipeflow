# PipeFlow — PipePipe fork design

Date: 2026-10-08
Status: draft, awaiting review

## Goal

Personal fork of PipePipe (Android, NewPipe-derived) with two behavior changes:

1. Downloaded items play from the local file instead of streaming, so a local playlist plays fully offline in background mode.
2. The "Recommended Lives" kiosk (YouTube's only kiosk, browsing YouTube's Live channel) is gone from the app.

Plus the setup that makes it an independent app: own repo, own app id and name, no upstream update prompts.

## Decisions

| Topic | Decision |
|---|---|
| Repo | One public GitHub fork of `InfinityLoop1308/PipePipeClient` → `lxhan/pipeflow`. Upstream wrapper repo dropped. Extractor consumed as an untouched submodule pointing at upstream. |
| Base commit | Client `c2a166f` (v5.4.0, the commit upstream's wrapper pins), not `dev` HEAD. |
| App identity | `applicationId dev.lxhan.pipeflow`, label "PipeFlow". Installs next to stock PipePipe; data moves via PipePipe's export/import backup. |
| Lives | Strip the "Recommended Lives" kiosk for all services from tabs, drawer, tab picker. Client-only change. |
| Offline modes | Background (audio) player only is the target. Other modes get the local file too as a side effect but are not verified. |
| Local preference | Always use the local file when one exists, online or offline. No setting. |
| Scope of local playback | Any play queue (local playlist, remote playlist, history), because the hook sits in the player. |
| Offline, not downloaded | Skip the item immediately. |
| Offline extras | Resume position and watch history keep working. Notification thumbnail may be blank. |
| Update checker | Removed (startup prompt and worker). |
| Signing | Existing env-var signing in `app/build.gradle`; keystore lives outside the repo. |

## 1. Repo and build setup

### GitHub and local layout

- `gh repo fork InfinityLoop1308/PipePipeClient --fork-name pipeflow --clone=false` → `lxhan/pipeflow`.
- In the fork, create branch `main` at `c2a166f` and make it the default branch. Upstream's `dev` branch stays in the fork untouched.
- `/Users/lx/github/lxhan/pipeflow` becomes a clone of the fork. Remotes: `origin` = `lxhan/pipeflow`, `upstream` = `InfinityLoop1308/PipePipeClient`. The current wrapper clone in that directory is replaced.

### Extractor

- Add submodule `PipePipeExtractor/` → `https://github.com/InfinityLoop1308/PipePipeExtractor.git`, pinned at `c68e10e` (upstream wrapper's v5.4.0 pin).
- `settings.gradle`: `includeBuild('../PipePipeExtractor')` → `includeBuild('PipePipeExtractor')`. Required because `app/build.gradle` declares the extractor dependency without a version; it only resolves through the composite build.
- Clone + build: `git clone --recursive git@github.com:lxhan/pipeflow.git`.

### Upstream sync

GitHub's "Sync fork" button tracks upstream's default branch (`dev`), which is not our base, so it is not the sync path. Instead `scripts/sync-upstream.sh`:

1. Reads the client and extractor commits upstream's wrapper currently pins: `gh api repos/InfinityLoop1308/PipePipe/contents/PipePipeClient --jq .sha` (same for `PipePipeExtractor`).
2. `git fetch upstream` and `git merge <client sha>` into `main`. Stops on conflict for manual resolution.
3. Checks the extractor submodule out at the pinned sha, stages it, commits `bump extractor to <short sha>`.

This keeps client and extractor paired the way upstream releases them.

### App identity

`app/build.gradle`:

- `applicationId "dev.lxhan.pipeflow"`.
- `resValue "string", "app_name"`: release "PipeFlow", debug "PipeFlow Debug" (keeps `.debug` suffix), `packageSuffix` variant "PipeFlow <suffix>".
- APK output name prefix `PipePipe_` → `PipeFlow_`.
- Code namespace `org.schabi.newpipe` unchanged to keep upstream merges small.
- Manifest provider authority already uses `${applicationId}`; no hardcoded package references found in `res/` or `java/`.

### Update checker

`MainActivity`: remove the `NewVersionWorker.enqueueNewVersionCheckingWork` call and the "enable update checker?" dialog. The first-run block that showed the dialog also requests the notification permission; that request stays. The update settings screen code stays (smaller diff, unreachable in practice since the pref defaults to off). Donation and announcement dialogs are untouched.

### Signing

`app/build.gradle` already builds a release `signingConfig` from env vars `KEY_PATH`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. No gradle change. Keystore generated once with `keytool`, stored at `~/.android/pipeflow.jks`, never committed. ABI splits produce per-ABI APKs; phones use `arm64-v8a`.

### AGENTS.md

New `AGENTS.md` at repo root with a `## Scripts` section:

- Env: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, `ANDROID_HOME=~/Library/Android/sdk`.
- Build release: `./gradlew assembleRelease`.
- Unit tests: `./gradlew testDebugUnitTest`.
- Lint: `./gradlew lintDebug`.
- Install: `adb install -r app/build/outputs/apk/release/PipeFlow_*-arm64-v8a-release.apk`.
- Upstream sync: `scripts/sync-upstream.sh`.

## 2. Strip "Recommended Lives"

The kiosk id `"Recommended Lives"` is shared by YouTube (only and default kiosk), BiliBili and NicoNico. The extractor is left alone: removing YouTube's kiosk there leaves no default kiosk, and `KioskList` then silently falls back to a BiliBili link handler.

- New `util/KioskFilter.kt` (`object`, `@JvmStatic` members): `isHidden(kioskId)` → `kioskId == "Recommended Lives"`; `visibleKiosks(service)` → the service's available kiosks minus hidden ones; `filterTabs(tabs, defaultKioskId)` (pure, unit-tested); `selectedServiceDefaultKioskId(context)`.
- `MainActivity` drawer: both kiosk loops (`:310` builds menu items with index ids, `:397` maps a tapped index back to a kiosk id) iterate `visibleKiosks(service)`, so the index mapping stays consistent.
- `SelectKioskFragment` (`:109`): iterate `visibleKiosks(service)`, so it cannot be added as a tab.
- `TabsJsonHelper.FALLBACK_INITIAL_TABS_LIST`: `SUBSCRIPTIONS`, `BOOKMARKS` (drop `DEFAULT_KIOSK`).
- `TabsManager.getTabs()`: after parsing, drop `KioskTab` whose kiosk id is hidden, and `DefaultKioskTab` when the current service's default kiosk id is hidden. If that leaves nothing, return the fallback list. This covers saved tabs and imported PipePipe backups.
- `ChooseTabsFragment` "add tab" list: omit `DEFAULT_KIOSK` when the current service's default kiosk is hidden.

Result for YouTube: no kiosk anywhere; home opens on Subscriptions. BiliBili/NicoNico keep their other kiosks.

## 3. Offline playback: local-first player hook

### How playback works today

Every queue item (`PlayerMediaItem`) is resolved in `MediaSourceManager.getLoadedMediaSource` through `ExtractorStreamInfoResolver` → `ExtractorHelper.getStreamInfo`, a network call. Downloads are recorded in a separate SQLite file `downloads.db`, table `finished_missions(path, url, bytes_downloaded, timestamp, kind)`: `path` is a `file://` or SAF `content://` URI, `url` is the canonical stream page URL (for YouTube `https://www.youtube.com/watch?v=<id>`, the same form `StreamEntity.url` holds), `kind` is `a`/`v`/`s`. Each video download is one muxed file. Nothing reads these rows for playback today.

### Units

1. **`FinishedMissionStore.findByUrl(String url)`** (`us/shandian/giga/get/sqlite`): `SELECT path, kind FROM finished_missions WHERE url = ? ORDER BY timestamp DESC`, returns `List<DownloadedFile>` (`player/local/DownloadedFile(path: String, kind: Char)`). Does not build `StoredFileHelper` per row (the existing loader does, which is slow for SAF).

2. **`player/local/LocalStream`**: value holding `audioPath: String?`, `videoPath: String?`, `entity: StreamEntity?`. Paths are kept as strings so the selection logic is JVM-testable. `fromFiles(files, exists, entity)` keeps `a`/`v` rows whose file exists and takes the newest of each kind; null when none. `pick(audioOnly)` returns audio-first when `audioOnly`, video-first otherwise, falling back to the other.

3. **`player/local/LocalStreamLookup`**: `find(item): Maybe<LocalStream>`. Queries `findByUrl(item.url)`, checks existence (`File.exists()` for `file://` and scheme-less legacy paths, `DocumentFile.fromSingleUri(...).exists()` for `content://`), loads the `StreamEntity` for `(serviceId, url)` from the Room `streams` table if present. Empty when no playable file or on any error. Runs on `Schedulers.io()`.

4. **`player/local/LocalStreamInfo.from(PlayerMediaItem item, @Nullable StreamEntity entity)`**: builds a `StreamInfo` with no streams and no related items via `new StreamInfo(serviceId, url, url, streamType, id, name, 0)` plus setters for uploader name/url, thumbnail url, duration, view count, upload date. Values come from `entity` when present (always the case for local playlist items, since playlist entries reference `streams`), otherwise from `item`. `id` comes from the service's stream link handler `getId(url)`, falling back to `url` if parsing throws. Seeding from `entity` matters because history writes upsert a `StreamEntity` built from this info; queue-item-only values would reset view count.

5. **`PlaybackListener.localSourceOf(PlayerMediaItem item, LocalStream local)`**, implemented by `PlaybackListenerAdapter` → `Player.localSourceOf`: picks the path with `local.pick(isAudioOnly || audioPlayerSelected())`, builds the synthetic info, tags with `PlayerMediaItem.forStreamInfo(info)`, and returns `dataSource.getProgressiveMediaSourceFactory().createMediaSource(MediaItem.Builder().setUri(uri).setTag(tag).build())`. The progressive factory is backed by `DefaultDataSource`, which handles `file://`, scheme-less paths and `content://`.

6. **`MediaSourceManager.getLoadedMediaSource`**: first `localStreamLookup.find(stream)`; on hit, `localSourceOf` → `LoadedMediaSource(source, tag, stream, Long.MAX_VALUE)` (never expires, so it is never re-resolved). If the lookup is empty or `localSourceOf` returns null, the existing network path runs unchanged.

7. **Offline skip**: in the network path's `onErrorReturn`, before the existing error-type checks: if the active network lacks `NET_CAPABILITY_INTERNET` or `NET_CAPABILITY_VALIDATED` (`VALIDATED` makes captive portals and dead Wi-Fi count as offline), return a `FailedMediaSource` carrying a new `OfflineSkipException extends FailedMediaSourceException`, retryable after 30 s so items become playable again once the network is back. The check runs first because an offline failure can surface either as a raw `IOException` or wrapped in an `ExtractionException`. `FailedMediaSource` plays 1 s of silence for that type instead of 2 s (its own docs warn that under 1 s can make ExoPlayer buffer). Today the raw-`IOException` case throws into the player, which triggers `recoverFromNetworkError` and a reload loop.

8. **No error notification for offline skips**: `PlayerMetadataController.onEvents` posts an error notification for every media item carrying errors. It skips that when every error is an `OfflineSkipException`, so an offline playlist does not spam one notification per undownloaded item.

9. **Resume**: `HistoryRecordManager.loadStreamState(PlayerMediaItem)` looks up the `StreamEntity` by `(serviceId, url)` in the local `streams` table first and reads its state; only on a miss does it fall back to the current extractor call. Removes a network dependency from resume for online playback as well.

**Player-mode switches need no change.** `PlayerSourceController.useVideoSource` reloads the queue manager when the current item has no `StreamInfo` or when its source type can't be toggled by track selection. With the synthetic info present, a reload just re-runs `getLoadedMediaSource`, whose local lookup now picks the file matching the new mode (e.g. the video file when leaving background), resuming at the recovery position. That is the desired behavior, so local items get no special-casing there and no marker key in `ItemKeys`.

### Data flow (background, offline, local playlist)

`LocalPlaylistFragment` → `SinglePlayQueue` → `PlayerService` → `MediaSourceManager.getLoadedMediaSource(item)` → `LocalStreamLookup.find` hit → `Player.localSourceOf` → progressive source on file URI with synthetic `StreamInfo` in the tag → playback; history, resume, notification title/artist work from that info. On a miss with no network → `OfflineSkipException` → 1 s of silence, no notification → next item.

Background mode already disables the video track type at start (`PlayerStartController`), so a muxed video file played in background decodes audio only.

## Error handling

- Lookup failures (DB error, permission revoked on a SAF URI) → treated as no local file; network path runs.
- File deleted after lookup → ExoPlayer source error at prepare; existing player error handling applies. Not specially handled.
- Online, no local file → behavior unchanged from upstream.

## Testing

Unit tests (JUnit 4, already a `testImplementation` dependency; `app/src/test` is new):

- `LocalStream.fromFiles`: subtitles and unknown kinds ignored, missing files skipped, newest existing file per kind wins, null when nothing playable.
- `LocalStream.pick`: audio-only/video preference and fallback for each combination of present paths.
- `LocalStreamInfo.from`: fields from entity when present, from item otherwise; id fallback when the service lookup throws.
- `KioskFilter.isHidden` and `KioskFilter.filterTabs` (kiosk tab hidden, default kiosk tab hidden when the default id is hidden, kept otherwise, other tabs untouched, only-hidden input → empty list).

Manual on device (release build):

1. Airplane mode, local playlist mixing downloaded and non-downloaded items, Background play: downloaded items play in order, others skip without a visible stall, no error notification loop.
2. Stop mid-item, resume later offline: position restored.
3. Watch history shows the offline-played items with correct title/uploader and unchanged view count.
4. During local playback, switch background → popup: no reload loop; the video file plays if one is downloaded, from the same position.
5. Online: downloaded item in a queue starts instantly from the file; non-downloaded streams normally.
6. Fresh install: no Recommended Lives in home tabs, drawer, or add-tab picker. Import a PipePipe backup whose tabs include it: still absent.
7. No update-checker prompt on first launch.

## Non-goals

- Main-player offline playback (the video detail screen needs network metadata to open).
- Download badges in playlist UI, offline-only queue filtering, a "prefer downloaded" setting.
- Filtering live streams out of related videos, autoplay, search, or the feed.
- Rebranding beyond app id and label (icons, package namespace, strings).
- CI builds or published releases.

## Known limitations

- Notification and lock-screen thumbnail are blank offline (thumbnail is a remote URL).
- Autoplay does not append related videos after a local item (synthetic info has no related items).
- Lookup is by exact URL. BiliBili `?p=` and `#timestamp=` variants, and non-canonical URLs in imported data, may miss and stream instead. YouTube is unaffected.

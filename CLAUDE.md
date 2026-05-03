# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "org.zotero.android.SomeTestClass"

# Clean
./gradlew clean
```

# Install to connected device (USB debugging must be on)
.\gradlew.bat installDevDebug   # Windows — run in PowerShell with JAVA_HOME set
./gradlew installDevDebug       # Linux/macOS

Lint is configured permissively (`checkReleaseBuilds = false`, `abortOnError = false`) — lint warnings do not fail builds.

**Environment**: JDK 17, Kotlin 2.2.0, Gradle 8.12.0, Min SDK 23, Target/Compile SDK 35.

## Architecture Overview

Single-module Android app (`app/`) with a feature-based package layout under `org.zotero.android`.

### MVVM + Compose

ViewModels extend `BaseViewModel2<STATE, EFFECT>` where `STATE` is an immutable data class and `EFFECT` is a sealed class for one-shot events. UI screens implement the `Screen` interface and observe state/effects via LiveData. Effects use a `Consumable` wrapper to prevent re-emission on configuration changes.

```kotlin
// Pattern used throughout screens/
class FooViewModel @HiltViewModel constructor(...) : BaseViewModel2<FooViewState, FooViewEffect>(FooViewState()) {
    fun doSomething() {
        updateState { copy(isLoading = true) }
        triggerEffect(FooViewEffect.NavigateBack)
    }
}
```

### Dependency Injection

Dagger 2 + Hilt. Activities are annotated `@AndroidEntryPoint`, ViewModels with `@HiltViewModel`. DI modules live in `architecture/di/`.

### Database

Realm NoSQL (v10.19.0). Two databases:
- **Main** (`DbWrapperMain`) — user library data
- **BundledData** — static reference data (citation styles, etc.)

Database operations follow a request object pattern:

```kotlin
// Read
val result = dbWrapperMain.realmDbStorage.perform(request = ReadItemDbRequest(key = key))
// Write
dbWrapperMain.realmDbStorage.perform(request = StoreItemDbRequest(item = item))
```

Migrations are in `database/migrations/` as separate classes.

### Networking

Retrofit 3 + OkHttp 5. Four separate API interfaces: `ZoteroApi`, `AuthApi`, `NonZoteroApi`, `WebDavApi`. Each has its own DI module and OkHttp client configured in `architecture/di/`.

### Sync Engine

The core sync logic is in `sync/`. `SyncEngine` orchestrates sync actions defined as a sealed class `SyncAction`. `SyncRepository` handles conflict resolution. Background sync runs via `SyncScheduler`.

### Key Libraries

- **PSPDFKit/Nutrient** (2024.4.0) — proprietary PDF viewer, used in `pdf/`
- **EventBus** — cross-cutting events alongside LiveData
- **Coil 3** — image loading in Compose
- **Realm** — database (not Room)
- **Timber** — logging
- **Firebase Crashlytics** — crash reporting

## Key Packages

| Package | Responsibility |
|---|---|
| `api/` | Retrofit API interfaces and models |
| `architecture/` | Base classes, DI modules, coroutine scopes |
| `database/` | Realm schema, migrations, request objects |
| `screens/` | Feature screens (each has ViewModel + composable) |
| `sync/` | Sync engine, conflict resolution, scheduler |
| `pdf/` | PDF reader/annotation via PSPDFKit |
| `translator/` | Web translator integration (runs in WebView) |
| `uicomponents/` | Shared Compose components |
| `attachmentdownloader/`, `backgrounduploader/` | File transfer |
| `webdav/`, `websocket/` | Alternative sync protocols |

## Custom Attachment Folder (branch: `local_folder`)

Allows the app to read/write PDF attachments from a user-picked SAF directory (e.g. a folder synced by Syncthing) instead of app-internal storage.

**Entry points:**
- `Defaults.getCustomAttachmentDirectoryUri()` / `setCustomAttachmentDirectoryUri()` — persists the SAF tree URI in SharedPreferences
- `FileStore.findFileInCustomDirectory()`, `copyFromCustomDirectory()`, `writeToCustomDirectory()` — SAF I/O helpers using `DocumentFile` + `ContentResolver`
- `SettingsScreen` + `SettingsViewModel` — "Storage" section; tap launches `OpenDocumentTree`, long-press clears; calls `takePersistableUriPermission` for reboot persistence
- `PdfReaderViewModel.initFileUris()` — checks custom dir first; copies SAF file to dirty location if found
- `PdfReaderViewModel.onStop()` → `saveToCustomDirectoryIfNeeded()` — writes dirty file back to SAF URI on reader close

File lookup is flat (root of the SAF tree only), matched by filename.

## Build Variants

- `dev` — debug variant, app name "Zotero Debug"
- `internal` — internal Play Store track
- Release signing requires `zotero.release.keystore` and `keystore-secrets.txt` (not in repo)

## CI/CD

GitHub Actions (`.github/workflows/android.yml`) triggers on tag push. It decrypts keystores via OpenSSL, runs Python bundling scripts for translators/styles/PDF worker/citation processor, then builds and publishes to Google Play Internal Test Track.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

T9Launcher is an Android launcher app that provides T9-style app search. It appears as a translucent overlay card with a numeric keyboard (2-9 keys + clear) and a horizontal scrollable list of matching apps.

- **Min SDK:** 33, **Target/Compile SDK:** 36
- **Package:** `fasolato.click.t9launcher`
- **Language:** Kotlin, **Build:** Gradle with Kotlin DSL

## Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build signed release APK (requires env vars — see Signing section)
./gradlew assembleRelease

# Build release bundle for Play Store
./gradlew bundleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Install on connected device
./gradlew installDebug
```

## Architecture

Single-activity app with no fragments. All logic lives in `app/src/main/java/fasolato/click/t9launcher/`.

**`MainActivity.kt`** — Entry point and orchestrator. Handles:
- T9 digit input and mapping (2→abc, 3→def, ... 9→wxyz)
- Loading installed apps on a background thread (with skeleton state during load)
- Filtering apps by T9 match and re-sorting on each keypress
- Smart card positioning relative to the launcher shortcut source bounds
- App launch tracking via `LaunchTracker`
- Broadcast receiver for package install/remove/replace events
- Long-press context menu (App Info, Uninstall)

**`AppAdapter.kt`** — RecyclerView adapter for the app list. Handles:
- Sorting: by launch count descending (last 10 days), then name ascending
- T9 match highlighting: yellow background + bold on matched characters in the app name

**`LaunchTracker.kt`** — Persists launch timestamps in SharedPreferences as JSON. Counts launches in the last 10 days; prunes old entries on each write.

**`OptionsActivity.kt`** — Settings screen for user preferences.

**`OptionsRepository.kt`** — Persistence layer for user settings.

**`SettingsAdapter.kt`** / **`SettingsEntry.kt`** — Adapter and model for the settings list UI.

**`SettingsRepository.kt`** — Hardcoded list of Android settings shortcuts (45+ entries) with Italian labels mapped to `Intent` actions. Not currently integrated in the main UI after the settings mode was removed.

**`SkeletonAdapter.kt`** — Placeholder adapter shown while the app list loads asynchronously.

**`AppPageAdapter.kt`** — ViewPager2 adapter, likely for paginated app display.

## T9 Matching Logic

App names are split by delimiters (space, dash, underscore, dot). T9 digits are matched against the first characters of each word segment. The match is prefix-based per segment.

## UI / Theming

- The activity uses `Theme.T9Launcher.Launcher` (translucent with 0.6 dim) to create an overlay popup effect.
- `activity_main.xml` defines the card layout with the T9 keyboard grid and a horizontal `RecyclerView`.
- Keyboard key style is defined via `T9Key` style in `themes.xml`.
- i18n: English (`values/strings.xml`) and Italian (`values-it/strings.xml`).

## Dependency Management

Dependencies are declared in `gradle/libs.versions.toml` (version catalog). Add new dependencies there first, then reference via `libs.*` aliases in `app/build.gradle.kts`.

## Signing

The release build uses a `signingConfigs` block in `app/build.gradle.kts` that reads from environment variables:

| Env var | Description |
|---------|-------------|
| `KEYSTORE_PATH` | Absolute path to `t9launcher.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias (`t9launcher`) |
| `KEY_PASSWORD` | Key password |

Locally, credentials are in `keystore.properties` (gitignored). The keystore file is `t9launcher.jks` (gitignored, at repo root).

## CI/CD

`.github/workflows/build.yml` triggers on every push to `main` and produces a signed release APK uploaded as a GitHub Actions artifact (7-day retention).

Required GitHub Secrets: `KEYSTORE_BASE64` (base64-encoded `.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Uses JDK 21 (matching `gradle/gradle-daemon-jvm.properties` `toolchainVersion=21`) and AGP 9.1.0 / Gradle 9.3.1.

## Release Guide

See `TONY.md` for the manual steps to publish on Google Play Store (keystore creation, bundle build, Play Console setup, privacy policy requirements).

# Trove

Local-first Android app that turns your screenshot folder into a searchable library: on-device OCR, automatic categories, collections, and full-text search. Nothing leaves the device — no network calls, no analytics unless you opt in.

## Stack

Kotlin, Jetpack Compose + Material 3 (dynamic color, edge-to-edge), MVVM, Room (+ FTS4 for search, Paging 3 for grids), WorkManager for resumable background indexing, ML Kit Text Recognition (on-device), Coil, Hilt.

## Building

This environment has no Android SDK / Gradle installed, so the project has not been compiled here. To build:

1. Open the project root in Android Studio (Koala+) — it will generate the Gradle wrapper jar and sync automatically.
   - Or from a machine with Gradle installed: `gradle wrapper --gradle-version 8.10.2`, then `./gradlew assembleDebug`.
2. Minimum SDK 26, target/compile SDK 35.

## Project layout

- `data/local/db` — Room entities, DAOs, `AppDatabase` (categories seeded on create).
- `data/mediastore` — screenshot discovery via `MediaStore.Images.Media` (matches the `Screenshots` bucket / `Screenshot_*` filenames).
- `data/ocr` — `OcrEngine` interface + ML Kit implementation.
- `data/classification` — `Classifier` interface + deterministic keyword-based implementation.
- `data/indexing` — `IndexingWorker` (resumable: re-queries "what's left to OCR/classify" from Room on every run) + `IndexingManager` (WorkManager scheduling + progress).
- `data/repository` — the only layer ViewModels talk to; Room/MediaStore/ML Kit never leak past it.
- `ui/*` — one package per screen, each with a `HiltViewModel` and Compose UI.

## What's implemented (MVP, per the brief's own milestone list)

Screenshot discovery, grid + timeline library views, full-screen pinch-zoom swipeable viewer, Room-backed OCR indexing, FTS4 search, automatic categories, collections, screenshot details (notes/tags/OCR text/copy), light/dark/dynamic theme, settings + privacy page. Exact-duplicate detection is included (cheap SQL group-by on size+dimensions) with manual review before any deletion — deletion always goes through the OS system picker (`MediaStore.createDeleteRequest`), never silently.

## Deliberately deferred (interfaces are ready, implementations aren't)

Perceptual/visual similarity, semantic (embedding) search, on-device generative categorization (Gemini Nano), summaries/auto-titles, cross-device sync, desktop companion. Each has a seam already in place — `Classifier`, `SearchRepository`, and the FTS `search_index` table are swappable without touching the UI or schema — see the doc comments on those interfaces.

## Releasing (IzzyOnDroid / Orion Store)

Licensed GPL-3.0-only. Store listing text and icon live in `fastlane/metadata/android/en-US`.

1. Bump `versionCode`/`versionName` in `app/build.gradle.kts` and add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
2. `./gradlew assembleRelease` (JDK 17). Signing reads `keystore.properties` (gitignored, points at the keystore). Keep the same key for every release.
3. Attach `app/build/outputs/apk/release/app-release.apk` to a GitHub release. IzzyOnDroid picks it up from there.

ML Kit is closed source, so the main F-Droid repo will not accept this build. IzzyOnDroid lists it with a non-free dependency flag.

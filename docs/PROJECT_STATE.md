# Recall — Project State

**Last updated:** 2026-05-20, after Phases 0–2, 4  
**Status:** Foundation complete. UI skeleton, Room DB, and ML interfaces ready. Entering MediaStore + Vector Search.

## Current Phase
Phase 3 — MediaStore Integration (+ Phase 5 Vector Search interfaces in parallel)

## Last Completed Phases
- Phase 0 — Project Setup (multi-module Gradle, convention plugins)
- Phase 1 — UI Skeleton (RecallTheme, navigation, stub screens, permissions)
- Phase 2 — Room Database (entities, DAOs, type converters, Hilt DI)
- Phase 4 — ML Embedding MVP (interfaces, MockEmbeddingModel, DeviceProfiler)

## Architecture

```
:app  (RecallApplication, MainActivity, NavHost, bottom nav)
├── :core:common          — RecallDispatchers
├── :core:database        — RecallDatabase (Room), 6 entities, 6 DAOs, Hilt module
├── :core:media           — [PENDING] MediaStore scanner, thumbnails, keyframes
├── :core:ml              — EmbeddingModel interface, MockEmbeddingModel, DeviceProfiler, ModelProfileSelector
├── :core:vector          — [PENDING] VectorIndex interface, LinearScanIndex
├── :core:worker          — [PENDING] WorkManager workers
├── :core:designsystem    — RecallTheme (dark-first), common composables (SearchBar, MediaGridItem, etc.)
├── :feature:search       — SearchScreen (stub), SearchViewModel
├── :feature:timeline     — TimelineScreen (stub), TimelineViewModel
├── :feature:detail       — MediaDetailScreen (stub), MediaDetailViewModel
├── :feature:settings     — SettingsScreen (stub), SettingsViewModel
└── :feature:onboarding   — OnboardingScreen (real permissions), OnboardingViewModel
```

## Build Configuration

| Setting | Value |
|---------|-------|
| Package | `com.recall.app` |
| compileSdk | 36 |
| minSdk | 28 |
| targetSdk | 36 |
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| KSP | 2.3.7 |
| Compose BOM | 2026.02.01 |
| Hilt | 2.59.2 |
| Room | 2.8.4 |
| Navigation | 2.9.8 |
| WorkManager | 2.11.2 |
| Coil | 3.4.0 |
| TFLite | 2.17.0 |

## Room Database (v1)

Entities: `MediaItemEntity`, `IndexingJobEntity`, `VectorSegmentEntity`, `VectorPostingEntity`, `AppSettingEntity`, `ModelProfileEntity`

DAOs: `MediaItemDao`, `IndexingJobDao`, `VectorSegmentDao`, `VectorPostingDao`, `AppSettingDao`, `ModelProfileDao`

Schema exported to `core/database/schemas/`.

## ML Pipeline

- `EmbeddingModel` interface (embedImage, embedText)
- `MockEmbeddingModel` (deterministic pseudo-random, normalized vectors)
- `DeviceProfiler` (RAM, CPU, disk, NNAPI detection)
- `ModelProfileSelector` (Lite/Standard/Pro based on device capabilities)
- `ImagePreprocessor` (resize, RGB normalization)
- Real TFLite model integration deferred to later Phase 4 work

## Important Decisions

- Dark-first theme with warm amber (#F5A623) accent
- AGP 9.x: no explicit `kotlin-android` plugin (built into AGP)
- KSP 2.3.7 for AGP 9 compatibility
- `tensorflow-lite-support` removed due to LiteRT manifest conflict — will revisit
- MockEmbeddingModel used for integration testing until real model available

## Known Limitations

- All feature screens are stubs (no real data)
- No real ML model bundled (mock only)
- `:core:media` not yet implemented
- `:core:vector` not yet implemented
- `:core:worker` not yet implemented
- `tensorflow-lite-support` omitted due to namespace conflict

## Next Steps

1. **Phase 3** — MediaStore Agent: MediaScanner, ThumbnailLoader, KeyframeExtractor, ContentObserver
2. **Phase 5 prep** — Vector Search Agent: VectorIndex interface, LinearScanIndex
3. **Phase 5** — Search MVP: wire SearchScreen end-to-end
4. **Phase 8** — WorkManager Agent: background indexing pipeline
5. **Phase 6** — HNSW implementation

## Build/Test Status

- `./gradlew assembleDebug`: **PASS** (418 tasks)
- Unit tests: ConvertersTest (database), MockEmbeddingModelTest, ModelProfileTest (ml)
- No lint errors

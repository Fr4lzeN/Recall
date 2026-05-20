# Recall — Project State

**Last updated:** 2026-05-20, after Phases 0–10 (MVP backend complete)  
**Status:** MVP complete. End-to-end semantic search, background indexing, and recovery are implemented. Entering post-MVP phases (HNSW, segmented index, real ML model, feature polish).

## Current Phase

**Post-MVP** — Phase 11a (Timeline / Detail / Settings integration) is in progress in the working tree but not yet committed. Next planned work: Phase 6 (HNSW), Phase 7 (segmented vector storage), real TFLite model.

## Completed Work (11 commits on `main`)

| Phase | Commit (short) | Summary |
|-------|------------------|---------|
| 0 | `c764b3e` | Multi-module Gradle, convention plugins, version catalog |
| 1 | `900248f` | RecallTheme, NavHost, bottom nav, stub feature screens, onboarding permissions |
| 2 | `b0ebfa3` | Room v1: 6 entities, 6 DAOs, Hilt `DatabaseModule`, exported schema |
| 3 | `2abc4a2` | MediaStore: `MediaScanner`, `ThumbnailLoader`, `KeyframeExtractor`, `MediaContentObserver`, `MediaSyncManager` |
| 4 | `982944a` | `EmbeddingModel`, `MockEmbeddingModel`, `DeviceProfiler`, `ModelProfileSelector`, `ImagePreprocessor` |
| 5 (vector) | `b762a43` | `VectorIndex`, `LinearScanIndex`, `VectorDistance`, `DeletionBitmap`, segment placeholders |
| 5 (search) | `e275cf2` | `SearchViewModel` + `SearchScreen` wired to embed → search → Room lookup |
| 8 | `8d2375e` | WorkManager: `MediaScanWorker`, `EmbeddingWorker`, `IndexingPipelineManager` |
| 9 | `197c4fa` | `IntegrityCheckWorker`, `StartupIntegrityChecker`, `FailedJobRequeuer` |
| 0.1 | `ee39cf1` | `RecallDispatchersTest`, Hilt test discovery fix |
| 10 | `424482c` | 68 JVM unit tests (common, database, ml, vector) |

## Architecture (module graph)

```
:build-logic          convention plugins (library, feature, application, compose, hilt)
:app                  Hilt Application, MainActivity, NavHost, VectorModule, AppStartupInitializer
├── :core:common           RecallDispatchers, CommonModule
├── :core:database         RecallDatabase (Room v1), 6 entities, 6 DAOs, DatabaseModule
├── :core:designsystem     RecallTheme, SearchBar, MediaGridItem, TopBar, Loading/Empty/Error
├── :core:media            MediaScanner, ThumbnailLoader, KeyframeExtractor, MediaContentObserver, MediaSyncManager
├── :core:ml               EmbeddingModel, MockEmbeddingModel, DeviceProfiler, ModelProfileSelector, MlModule
├── :core:vector           VectorIndex, LinearScanIndex, VectorDistance, DeletionBitmap, SegmentManifest (stub)
├── :core:worker           MediaScanWorker, EmbeddingWorker, IntegrityCheckWorker, IndexingPipelineManager, recovery
├── :feature:search        SearchScreen + SearchViewModel (fully wired)
├── :feature:timeline      TimelineScreen (UI stub, placeholder grid)
├── :feature:detail        MediaDetailScreen (UI stub)
├── :feature:settings      SettingsScreen (static list UI, stub ViewModel)
└── :feature:onboarding    OnboardingScreen (real READ_MEDIA_* permission flow)
```

Dependency direction: **features → core** (never core → feature). `:app` aggregates features and binds app-level singletons (`VectorModule`).

## Build Configuration

| Setting | Value |
|---------|-------|
| Package / applicationId | `com.recall.app` |
| Version | `0.1.0` (versionCode 1) |
| compileSdk | 36 |
| minSdk | 28 |
| targetSdk | 36 |
| Java | 11 |
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| KSP | 2.3.7 |
| Compose BOM | 2026.02.01 |
| Hilt | 2.59.2 |
| Room | 2.8.4 |
| Navigation Compose | 2.9.8 |
| WorkManager | 2.11.2 |
| Coil | 3.4.0 |
| Coroutines | 1.10.2 |
| TFLite | 2.17.0 (dependency present; real model not bundled) |

Convention plugins in `:build-logic`: `recall.android.library`, `recall.android.feature`, `recall.android.application`, `recall.android.compose`, `recall.hilt`.

## Room Schema (v1)

**Database:** `RecallDatabase`, version 1, schema exported to `core/database/schemas/`.

| Entity | Table | Role |
|--------|-------|------|
| `MediaItemEntity` | `media_items` | MediaStore metadata, indexing flags, future segment mapping |
| `IndexingJobEntity` | `indexing_jobs` | Per-item embed job queue (FK → media_items, CASCADE) |
| `VectorSegmentEntity` | `vector_segments` | Future on-disk segment metadata (Phase 7) |
| `VectorPostingEntity` | `vector_postings` | Future id → segment/local index map (Phase 7) |
| `AppSettingEntity` | `app_settings` | Key-value app state (e.g. last scan timestamp) |
| `ModelProfileEntity` | `model_profiles` | Persisted Lite/Standard/Pro profile selection |

**Type converters:** `IndexingStatus` enum (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).

**DAOs:** `MediaItemDao`, `IndexingJobDao`, `VectorSegmentDao`, `VectorPostingDao`, `AppSettingDao`, `ModelProfileDao` — Flow observables + suspend queries.

## ML Pipeline Status

| Component | Status |
|-----------|--------|
| `EmbeddingModel` interface | Done (`embedImage`, `embedText`, `dimensions`, `profileName`) |
| `MockEmbeddingModel` | Done — deterministic hash-seeded vectors, L2-normalized |
| `DeviceProfiler` | Done — RAM, CPU, disk, NNAPI detection |
| `ModelProfileSelector` | Done — Lite (384d), Standard (512d), Pro (512d) |
| `ImagePreprocessor` | Done — resize, RGB normalization |
| Hilt binding | `MlModule` provides `MockEmbeddingModel` as `EmbeddingModel` |
| Real TFLite model | **Not integrated** — `tensorflow-lite` on classpath; `tensorflow-lite-support` deferred (LiteRT manifest conflict) |

## Vector Search Status

| Component | Status |
|-----------|--------|
| `VectorIndex` interface | Done |
| `LinearScanIndex` | Done — in-memory brute-force cosine similarity, mutex-protected |
| `VectorDistance` | Done — cosine similarity / distance helpers |
| `DeletionBitmap` | Done — soft-delete bookkeeping (tests) |
| `SegmentManifest` / `VectorSegment` | Placeholder interfaces for Phase 7 |
| App binding | `VectorModule` → `LinearScanIndex(embeddingModel.dimensions)` |
| HNSW | **Not started** (Phase 6) |
| Segmented on-disk index | **Not started** (Phase 7) |

## WorkManager Pipeline Status

| Component | Status |
|-----------|--------|
| `RecallApplication` | `Configuration.Provider` + `HiltWorkerFactory` |
| `AppStartupInitializer` | Enqueues full pipeline on cold start |
| `IndexingPipelineManager` | Unique chain: Integrity → Scan → Embed; periodic 6h scan |
| `MediaScanWorker` | MediaStore scan → upsert `media_items` → enqueue indexing jobs |
| `EmbeddingWorker` | Thumbnail → `embedImage` → `vectorIndex.add` → mark indexed |
| `IntegrityCheckWorker` | Runs `StartupIntegrityChecker` + `FailedJobRequeuer` |
| Constraints | Battery-not-low + storage-not-low on embed work |

## Consistency / Recovery Status

| Component | Status |
|-----------|--------|
| `StartupIntegrityChecker` | Requeue stuck `PROCESSING` jobs, clean `.tmp` in `files/segments`, purge completed jobs |
| `FailedJobRequeuer` | Requeue `FAILED` jobs with `retry_count < 3` |
| `EmbeddingWorker` | Requeues `PROCESSING` at start of each run |

## Feature UI Status (committed)

| Screen | Status |
|--------|--------|
| Search | **Complete** — debounced query, vector search, Coil thumbnails, indexed/total counts |
| Onboarding | **Complete** — READ_MEDIA_IMAGES/VIDEO (+ legacy READ_EXTERNAL_STORAGE ≤ API 32) |
| Timeline | **Stub** — placeholder grid, no Room data |
| Detail | **Stub** — title + placeholder content |
| Settings | **Stub** — static rows (model profile, storage, reindex) |

## Important Decisions Log

- **Privacy-first:** No `INTERNET` permission; all indexing and search on-device.
- **Dark-first UI** with warm amber (`#F5A623`) accent and violet secondary (`#7B61FF`).
- **AGP 9.x:** Kotlin Android plugin built into AGP; no separate `kotlin-android` apply.
- **KSP 2.3.7** for Room/Hilt compatibility with AGP 9.
- **String routes** for Navigation (not type-safe) for MVP simplicity.
- **In-memory `LinearScanIndex`** as singleton — acceptable for MVP; not persisted across process death (rebuilt by `EmbeddingWorker`).
- **Mock embeddings** until real CLIP-style model and golden vectors land.
- **`tensorflow-lite-support` omitted** — LiteRT namespace/manifest conflict; revisit with real model.
- **Room WAL** default; snake_case columns via `@ColumnInfo`.
- **CASCADE** FKs on `indexing_jobs` and `vector_postings`.
- **Startup indexing** runs automatically via `AppStartupInitializer` (may be heavy on first launch — tune in settings phase).

## Known Limitations

- **Mock ML only** — search quality is not semantically meaningful until a real embedding model ships.
- **Linear scan** — O(n) per query; suitable only for small libraries; no persistence of vectors to disk yet.
- **Vectors lost on process kill** — in-memory index; full re-embed required unless Phase 7 segment storage is added.
- **Video semantics** — keyframe extractor exists; embed path uses single thumbnail per item.
- **Timeline / Detail / Settings** — stubs at last commit; Phase 11a WIP in working tree.
- **No instrumented UI tests** beyond template `ExampleInstrumentedTest`.
- **Periodic scan** enqueued by pipeline manager but not yet exposed in Settings UI.

## Next Steps

1. **Phase 11a** — Wire Timeline, Detail, and Settings to Room + `IndexingPipelineManager` (in progress locally).
2. **Phase 6** — HNSW approximate nearest neighbor index.
3. **Phase 7** — Segmented on-disk vector storage (`VectorSegmentEntity`, `SegmentManifest` implementation).
4. **Real ML** — Bundle TFLite model, resolve `tensorflow-lite-support`, golden-vector tests.
5. **Performance** — Batch embed, GPU delegate, incremental index updates.
6. **Polish** — Settings-driven reindex, model profile UI, storage metrics.

## Build / Test Status

| Check | Result (last verified on `HEAD` `424482c`) |
|-------|---------------------------------------------|
| `./gradlew assembleDebug` | **PASS** |
| `./gradlew testDebugUnitTest` | **PASS** — **68** JVM tests |
| Test modules | `:core:common` (2), `:core:database` (16), `:core:ml` (14), `:core:vector` (36) |
| Lint | No dedicated lint gate documented |

**Note:** The working tree may contain uncommitted Phase 11a changes that do not compile until that work is finished. Verify with a clean `HEAD` checkout for release builds.

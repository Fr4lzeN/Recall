# Recall — Work Log

## 2026-05-20

### Phase 0 - Project Setup Agent - Initialize multi-module Gradle project
- **Summary:** Created multi-module Android project with 7 core + 5 feature modules, convention plugins, version catalog.
- **Files changed:** `build-logic/` (7 files), `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.gitignore`, all module `build.gradle.kts` + `AndroidManifest.xml` (24 files), `app/` sources (3 files), `core/common/RecallDispatchers.kt`, `docs/`
- **Decisions made:**
  - Package: `com.recall.app` (changed from `com.example.recall`)
  - AGP 9.x: no explicit `kotlin-android` plugin (built-in)
  - KSP 2.3.7 for AGP 9 compatibility
  - Hilt 2.59.2, Room 2.8.4, Navigation 2.9.8, WorkManager 2.11.2, Coil 3.4.0, TFLite 2.17.0
  - `tensorflow-lite-support` deferred due to LiteRT manifest conflict
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL (392 tasks)
- **Commit:** `c764b3e` — Phase 0 - Project Setup Agent: initialize Android multi-module Gradle project structure
- **Follow-ups:** Resolve `tensorflow-lite-support` LiteRT conflict

---

### Phase 1 - UI Compose Agent - RecallTheme, navigation, stub screens
- **Summary:** Created dark-first Material 3 theme, navigation graph with bottom nav (Search/Timeline/Settings), stub screens for all 5 feature modules, real permission handling in onboarding.
- **Files changed:**
  - `core/designsystem/`: theme (Color, Type, Shape, RecallTheme), components (SearchBar, MediaGridItem, TopBar, Loading/Empty/Error states)
  - `app/`: navigation (RecallRoute, RecallNavigation, RecallNavHost, TopLevelDestination), RecallApp, updated MainActivity, AndroidManifest (permissions)
  - `feature/search/`: SearchScreen, SearchViewModel
  - `feature/timeline/`: TimelineScreen, TimelineViewModel
  - `feature/detail/`: MediaDetailScreen, MediaDetailViewModel
  - `feature/settings/`: SettingsScreen, SettingsViewModel
  - `feature/onboarding/`: OnboardingScreen, OnboardingViewModel, MediaPermissions, PermissionState
  - Removed: `app/ui/theme/` (template files)
- **Decisions made:**
  - Dark theme as default with warm amber (#F5A623) accent, violet secondary (#7B61FF)
  - Bottom nav: Search (start), Timeline, Settings
  - Onboarding gate: shown when permissions not granted
  - String routes (not type-safe navigation) for simplicity
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL
- **Commit:** `900248f` — Phase 1 - UI Compose Agent: add RecallTheme, navigation graph, and stub screens
- **Follow-ups:** Wire real data into screens (Phase 3, 5)

---

### Phase 2 - Database Room Agent - Room entities, DAOs, database module
- **Summary:** Defined complete Room schema v1 with 6 entities, 6 DAOs, type converters, Hilt DI module. Schema exported.
- **Files changed:**
  - `core/database/entity/`: MediaItemEntity, IndexingJobEntity, VectorSegmentEntity, VectorPostingEntity, AppSettingEntity, ModelProfileEntity
  - `core/database/converter/Converters.kt`
  - `core/database/dao/`: MediaItemDao, IndexingJobDao, VectorSegmentDao, VectorPostingDao, AppSettingDao, ModelProfileDao
  - `core/database/RecallDatabase.kt`
  - `core/database/di/DatabaseModule.kt`
  - `core/database/src/test/`: ConvertersTest
  - `core/database/schemas/`: exported schema v1
- **Decisions made:**
  - Snake_case column names via `@ColumnInfo`
  - CASCADE foreign keys on indexing_jobs and vector_postings
  - Uri stored as String (no converter needed)
  - WAL mode default (no extra config)
- **Tests/checks:** `assembleDebug` PASS, `testDebugUnitTest` PASS (2 converter tests)
- **Commit:** `b0ebfa3` — Phase 2 - Database Room Agent: define Room entities, DAOs, and database module
- **Follow-ups:** Migration tests when schema changes, Room inspector verification on device

---

### Phase 4 - ML Embedding Agent - EmbeddingModel interface, mock, DeviceProfiler
- **Summary:** Defined EmbeddingModel interface, MockEmbeddingModel (deterministic, normalized vectors), DeviceProfiler, ModelProfileSelector, ImagePreprocessor, Hilt DI module.
- **Files changed:**
  - `core/ml/`: EmbeddingModel.kt, ModelProfile.kt, MockEmbeddingModel.kt, DeviceProfiler.kt, ModelProfileSelector.kt, ImagePreprocessor.kt, di/MlModule.kt
  - `core/ml/build.gradle.kts` (added test deps)
  - `core/ml/src/test/`: MockEmbeddingModelTest, ModelProfileTest
  - `core/ml/src/androidTest/`: MockEmbeddingModelAndroidTest, ImagePreprocessorAndroidTest
- **Decisions made:**
  - Mock embedding uses hash-based seed for determinism
  - L2 normalization on all output vectors
  - Lite (384d, INT8), Standard (512d, FP16), Pro (512d, FP32)
  - MVP uses MockEmbeddingModel via Hilt binding
  - Bitmap tests in androidTest (not available in JVM)
- **Tests/checks:** `assembleDebug` PASS, `testDebugUnitTest` PASS (7 tests)
- **Commit:** `982944a` — Phase 4 - ML Embedding Agent: define EmbeddingModel interface, mock, and DeviceProfiler
- **Follow-ups:** Real TFLite model integration, tensorflow-lite-support resolution, golden test vectors

---

### Phase 3 - MediaStore Agent - MediaScanner, thumbnails, keyframes, ContentObserver
- **Summary:** Implemented MediaStore scanning for images and videos, thumbnail loading (API 28–36), video keyframe extraction, content observer, permission helpers, and sync manager with Hilt `MediaModule`.
- **Files changed:**
  - `core/media/scanner/`: `MediaScanner`, `ScannedMediaItem`
  - `core/media/thumbnail/`: `ThumbnailLoader`
  - `core/media/keyframe/`: `KeyframeExtractor`
  - `core/media/observer/`: `MediaContentObserver`, `MediaChangeEvent`
  - `core/media/permission/`: `MediaPermissionHelper`
  - `core/media/sync/`: `MediaSyncManager`, `SyncResult`
  - `core/media/di/MediaModule.kt`
- **Decisions made:**
  - Full scan vs incremental via `AppSettingsKeys.LAST_MEDIA_SCAN_TIMESTAMP`
  - Images and videos merged into unified `ScannedMediaItem` model
  - IO dispatcher via `RecallDispatchers`
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL
- **Commit:** `2abc4a2` — Phase 3 - MediaStore Agent: implement MediaScanner, thumbnails, keyframes, and ContentObserver
- **Follow-ups:** Wire observer-driven incremental sync in UI settings

---

### Phase 5 - Vector Search Agent - VectorIndex interface and LinearScanIndex MVP
- **Summary:** Defined `VectorIndex` contract, brute-force `LinearScanIndex` with mutex, cosine distance utilities, `DeletionBitmap`, and segment manifest placeholders for Phase 7.
- **Files changed:**
  - `core/vector/`: `VectorIndex.kt`, `linear/LinearScanIndex.kt`, `distance/`, `bitmap/DeletionBitmap.kt`, `segment/`, `VectorIndexFactory.kt`
  - `app/di/VectorModule.kt` (app-level Hilt binding)
- **Decisions made:**
  - Cosine similarity as primary score (higher = more similar)
  - In-memory MVP index; segment types stubbed for future disk layout
  - `VectorModule` in `:app` to keep `:core:vector` free of Hilt
- **Tests/checks:** `assembleDebug` PASS
- **Commit:** `b762a43` — Phase 5 - Vector Search Agent: implement VectorIndex interface and LinearScanIndex MVP
- **Follow-ups:** HNSW (Phase 6), persist vectors to segments (Phase 7)

---

### Phase 5 - Search Integration Agent - Wire SearchScreen end-to-end
- **Summary:** Connected `SearchViewModel` to `EmbeddingModel.embedText`, `VectorIndex.search`, and `MediaItemDao`; built results grid with Coil, debounce, indexed/total counts, error/retry states.
- **Files changed:**
  - `feature/search/`: `SearchViewModel.kt`, `SearchUiState.kt`, `SearchScreen.kt`, `navigation/SearchNavigation.kt`
  - `app/navigation/RecallNavHost.kt` (search route extension)
- **Decisions made:**
  - 300 ms debounce on query changes
  - `topK = 50` for result cap
  - Observe `observeIndexedCount` / `observeCount` for empty-state messaging
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL
- **Commit:** `e275cf2` — Phase 5 - Search Integration Agent: wire SearchScreen end-to-end with vector search
- **Follow-ups:** Timeline/detail navigation to same media rows

---

### Phase 8 - WorkManager Agent - Background media scan and embedding workers
- **Summary:** Added Hilt WorkManager workers, unique work pipeline (integrity → scan → embed), periodic scan, and startup enqueue from `RecallApplication`.
- **Files changed:**
  - `core/worker/`: `MediaScanWorker`, `EmbeddingWorker`, `IntegrityCheckWorker`, `IndexingPipelineManager`, `AppSettingsKeys`, `di/WorkerModule.kt`
  - `app/`: `RecallApplication` (`Configuration.Provider`), `AppStartupInitializer`
- **Decisions made:**
  - `EmbeddingWorker` uses thumbnails, not full bitmaps
  - Batch processing with `BATCH_SIZE` limit per run
  - Battery-not-low + storage-not-low constraints on embed chain
  - `segmentId = 0` placeholder until Phase 7
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL
- **Commit:** `8d2375e` — Phase 8 - WorkManager Agent: add background media scan and embedding workers
- **Follow-ups:** Expose pipeline status in Settings UI

---

### Phase 9 - Consistency Recovery Agent - Startup integrity and failed job requeue
- **Summary:** Implemented `StartupIntegrityChecker` (stuck jobs, temp file cleanup, segments dir check), `FailedJobRequeuer` (retry cap 3), integrated into `IntegrityCheckWorker`.
- **Files changed:**
  - `core/worker/recovery/`: `StartupIntegrityChecker.kt`, `FailedJobRequeuer.kt`
  - `core/worker/IntegrityCheckWorker.kt` (updated)
- **Decisions made:**
  - Requeue all `PROCESSING` jobs on startup (crash recovery)
  - Clean `filesDir/segments/*.tmp` orphans
  - Max 3 retries for `FAILED` jobs
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL
- **Commit:** `197c4fa` — Phase 9 - Consistency Recovery Agent: add startup integrity checks and failed job requeue
- **Follow-ups:** Persist vector index to survive process death (Phase 7)

---

### Phase 0.1 - Orchestrator - Test discovery fix and RecallDispatchersTest
- **Summary:** Fixed Gradle `failOnNoDiscoveredTests` for Hilt-only modules; added `RecallDispatchersTest`.
- **Files changed:** `build-logic/convention/.../AndroidKotlin.kt`, `core/common/src/test/RecallDispatchersTest.kt`
- **Commit:** `ee39cf1` — Phase 0.1 - Orchestrator: fix test discovery for Hilt modules, add RecallDispatchersTest

---

### Phase 10 - Testing Agent - Comprehensive unit tests (68 JVM tests)
- **Summary:** Added DAO tests with Robolectric, vector index concurrency tests, distance edge cases, `DeletionBitmap`, `ModelProfileSelector` tests; total **68** JVM unit tests across core modules.
- **Files changed:**
  - `core/database/src/test/dao/`: `MediaItemDaoTest`, `IndexingJobDaoTest`, `AppSettingDaoTest`
  - `core/vector/src/test/`: `LinearScanIndexTest`, `LinearScanIndexConcurrencyTest`, `VectorDistanceTest`, `VectorDistanceEdgeCasesTest`, `DeletionBitmapTest`
  - `core/ml/src/test/`: `ModelProfileSelectorTest` (+ existing mock/profile tests)
- **Decisions made:**
  - In-memory Room + `InstantTaskExecutorRule` for DAO tests
  - Robolectric enabled via convention plugin `isIncludeAndroidResources`
- **Tests/checks:** `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL (68 tests)
- **Commit:** `424482c` — Phase 10 - Testing Agent: add comprehensive unit tests for DAO, vector, and ML modules
- **Follow-ups:** ViewModel tests, WorkManager integration tests

---

### Phase 11a - Feature Integration Agent - Timeline, Detail, Settings (in progress)
- **Summary:** WIP in working tree (not committed as of documentation update): wire Timeline to `MediaItemDao` Flow, enrich Detail screen, connect Settings to `IndexingPipelineManager` and model profile. Local changes may not compile until complete.
- **Status:** **In progress** — uncommitted changes under `feature/timeline`, `feature/detail`, `feature/settings`, `app/navigation/RecallNavHost.kt`
- **Tests/checks:** `./gradlew assembleDebug` passes on clean `HEAD` (`424482c`); WIP tree may fail compile until Phase 11a lands
- **Commit:** _(pending)_
- **Follow-ups:** Commit when compile-clean; add feature-level unit tests

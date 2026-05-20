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
- **Follow-ups:** Wire real data into screens (Phase 3, 5, 11a)

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
- **Follow-ups:** Expose pipeline status in Settings UI (done in Phase 11a)

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

### Phase 11a - Feature Integration Agent - Timeline, Detail, Settings
- **Summary:** Wired Timeline to `MediaItemDao.observeAll()` with date-grouped grid and indexing badges; Detail screen loads `mediaId` from nav args with preview and metadata; Settings shows indexing progress via `IndexingPipelineManager.observePipelineStatus()`, device/model info, **Re-index All** and **Clear Index** actions.
- **Files changed:**
  - `feature/timeline/`: `TimelineViewModel.kt`, `TimelineScreen.kt`, `navigation/TimelineNavigation.kt`, `build.gradle.kts` (+ `core:database`, `core:media`, Coil)
  - `feature/detail/`: `MediaDetailViewModel.kt`, `MediaDetailScreen.kt`, `navigation/DetailNavigation.kt`, `build.gradle.kts`
  - `feature/settings/`: `SettingsViewModel.kt`, `SettingsScreen.kt`, `build.gradle.kts` (+ `core:database`, `core:ml`, `core:vector`, `core:worker`)
  - `app/navigation/RecallNavHost.kt` (timeline + detail navigation callbacks)
- **Decisions made:**
  - Timeline groups by `dateTaken` (fallback `dateAdded` in seconds → ms)
  - Detail uses `SavedStateHandle` `mediaId` nav argument
  - `clearIndex()` resets Room `is_indexed` flags and calls `vectorIndex.clear()`
  - Model profile displayed read-only from `ModelProfileSelector.selectProfile()`
- **Tests/checks:** `./gradlew assembleDebug` — BUILD SUCCESSFUL
- **Commit:** `6e7136a` — Phase 11 - Feature Integration Agent: wire Timeline, Detail, and Settings with real data
- **Follow-ups:** ViewModel unit tests, model profile picker UI, wire `MediaContentObserver` for live gallery sync

---

### Phase 11 - Documentation Agent - README, architecture docs, project state
- **Summary:** Added root `README.md`, `docs/ARCHITECTURE.md`, refreshed `docs/PROJECT_STATE.md` and `docs/WORK_LOG.md` for MVP-complete state including Phase 11a.
- **Files changed:** `README.md`, `docs/ARCHITECTURE.md`, `docs/PROJECT_STATE.md`, `docs/WORK_LOG.md`
- **Tests/checks:** `./gradlew assembleDebug` + `testDebugUnitTest` — BUILD SUCCESSFUL
- **Commit:** _(this entry)_
- **Follow-ups:** Keep docs in sync when Phase 6/7 land

---

## Post-MVP (branch `post-mvp`)

### Phase 11b - UX Polish Agent - Animations, accessibility, R8 config
- **Summary:** Added app icon, splash screen, edge-to-edge display, accessibility labels, R8 ProGuard rules, Material Motion animations, `minimumInteractiveComponentSize`.
- **Branch:** `post-mvp`
- **Commit:** `0051776` — Phase 11 - UX Polish Agent: add animations, accessibility, R8 config, and UI polish

---

### Phase 6 - Vector Search Agent - Pure Kotlin HNSW implementation
- **Summary:** Implemented Hierarchical Navigable Small World (HNSW) algorithm in pure Kotlin with `add`, `search`, `remove`, `serialize`, `deserialize`. Dynamic `efSearch` scaling achieves recall@10 >= 0.95 vs brute force. Thread-safe with `ReentrantReadWriteLock`.
- **Files changed:**
  - `core/vector/hnsw/HnswIndex.kt`
  - `core/vector/src/test/`: `HnswIndexTest.kt`, `HnswRecallTest.kt`, `HnswConcurrencyTest.kt`
- **Tests/checks:** `testDebugUnitTest` — PASS (HNSW recall, concurrency, edge case tests)
- **Branch:** `post-mvp`
- **Commit:** `0408c04` — Phase 6 - Vector Search Agent: implement pure Kotlin HNSW with recall@10 >= 0.95

---

### Phase 6.1 - HNSW Integration Agent - Switch app to persistent HNSW
- **Summary:** Created `PersistentVectorIndex` wrapping `HnswIndex` with atomic file-based persistence (serialize → tmp → rename). Updated `VectorModule` in `:app` to use `PersistentVectorIndex`. Modified `EmbeddingWorker` to call `persist()` after batch processing.
- **Files changed:**
  - `core/vector/persistent/PersistentVectorIndex.kt`, `PersistableVectorIndex.kt`
  - `app/di/VectorModule.kt` (switched from LinearScan to PersistentVectorIndex)
  - `core/worker/EmbeddingWorker.kt` (added persist() call)
- **Tests/checks:** `assembleDebug` — PASS
- **Branch:** `post-mvp`
- **Commit:** `b5766cf` — Phase 6.1 - HNSW Integration Agent: switch to persistent HNSW index with file-based storage

---

### Phase 10b - Benchmark Agent - Comprehensive vector search benchmarks
- **Summary:** Added benchmark test suite for search latency (LinearScan vs HNSW at 1K–50K vectors), indexing throughput (single vs batch), recall@10 vs efSearch/M/scale, memory footprint, and serialization round-trip.
- **Files changed:**
  - `core/vector/src/test/.../benchmark/`: `BenchmarkUtils.kt`, `SearchBenchmarkTest.kt`, `IndexingBenchmarkTest.kt`, `RecallBenchmarkTest.kt`, `MemoryBenchmarkTest.kt`, `SerializationBenchmarkTest.kt`
- **Tests/checks:** Benchmark subset — PASS (1K/5K search, linear insert, serialization)
- **Branch:** `post-mvp`
- **Commit:** `9fd48e9` — Phase 10b - Benchmark Agent: add comprehensive vector search benchmark suite
- **Follow-ups:** Run full benchmark suite including 50K-vector tests on device

---

### Phase 4b - ML Embedding Agent - TFLite embedding model
- **Summary:** Added `TFLiteEmbeddingModel` using `org.tensorflow.lite.Interpreter` (no tensorflow-lite-support). Loads MobileCLIP from assets with NNAPI/GPU delegates, CLIP ImageNet preprocessing, L2-normalized embeddings. Text queries use a deterministic fingerprint bitmap routed through the image tower. `MlModule` probes assets and falls back to `MockEmbeddingModel` when `.tflite` files are absent.
- **Files changed:**
  - `core/ml/`: `TFLiteEmbeddingModel.kt`, `ImagePreprocessor.kt` (CLIP normalization), `di/MlModule.kt`
  - `core/ml/build.gradle.kts` (tensorflow-lite-gpu, Robolectric for unit tests)
  - `core/ml/src/test/`: `TFLiteEmbeddingModelTest.kt`
  - `docs/PROJECT_STATE.md`, `docs/WORK_LOG.md`
- **Tests/checks:** `./gradlew :core:ml:testDebugUnitTest` — PASS; `./gradlew :core:ml:compileDebugUnitTestKotlin` — PASS
- **Branch:** `post-mvp`
- **Follow-ups:** Bundle `mobileclip_*.tflite` in assets, androidTest golden-vector inference, dedicated text tower when support lib conflict is resolved

---

### Phase 7 - Segmented Vector Agent - Segmented HNSW with mmap reader
- **Summary:** Implemented on-disk segmented vector index: binary segment format with CRC32 checksum, atomic segment publishing (tmp → rename), mmap-based `SegmentReader` with checksum validation, HNSW search directly on frozen segments (`SegmentHnswSearch`), multi-segment `SegmentedVectorIndex` with in-memory staging buffer (flush at 1000 vectors) and merged cross-segment top-K search. Room-backed `SegmentManifest` and `VectorPostingStore` in `:app`.
- **Files changed:**
  - `core/database/dao/VectorSegmentDao.kt` — `setDeletedCount()` query
  - `core/vector/hnsw/HnswIndex.kt` — `exportEntries()`, `exportForSegment()`
  - `core/vector/hnsw/SegmentGraphExport.kt` — graph export data class
  - `core/vector/segment/`: `SegmentFormat.kt`, `SegmentWriter.kt`, `SegmentReader.kt`, `SegmentHnswSearch.kt`, `VectorPostingStore.kt`, `SegmentManifest.kt` (return type fix)
  - `core/vector/segmented/SegmentedVectorIndex.kt`
  - `app/data/vector/`: `RoomSegmentManifest.kt`, `RoomVectorPostingStore.kt`
  - `core/vector/src/test/segment/`: `SegmentFormatTest`, `SegmentWriterReaderTest`, `SegmentTestDoubles`
  - `core/vector/src/test/segmented/SegmentedVectorIndexTest`
- **Decisions made:**
  - **MappedByteBuffer over NDK** — pure Kotlin/JVM mmap for segment reads; no native code dependency
  - **VectorPostingStore interface** — keeps `:core:vector` free of Room; `:app` provides `RoomVectorPostingStore`
  - **Staging buffer flush threshold = 1000** — in-memory `HnswIndex` accumulates new vectors until threshold, then flushes to immutable on-disk segment
  - **Atomic publish** — `SegmentWriter` writes to `.tmp` then renames; aligns with `StartupIntegrityChecker` tmp cleanup
  - **CRC32 checksum** — segment footer validates integrity on mmap open
- **Tests/checks:** `./gradlew :core:vector:testDebugUnitTest` — PASS (format checksum, write/read roundtrip, cross-segment search + deletion); `./gradlew assembleDebug` — PASS
- **Branch:** `post-mvp`
- **Commit:** `53fe180` — Phase 7 - Segmented Vector Agent: add segmented HNSW with mmap reader and atomic publishing
- **Follow-ups:** Wire `SegmentedVectorIndex` in `VectorModule`, migrate existing `hnsw.idx` data to segments

---

### Phase 12 - DI Integration Agent - wire SegmentedVectorIndex into app Hilt graph
- **Summary:** Switched `VectorModule` from `PersistentVectorIndex` to `SegmentedVectorIndex.open()` with Room-backed `SegmentManifest` and `VectorPostingStore`. Added `PersistableVectorIndex` Hilt binding. Extended `SegmentedVectorIndex.clear()` to remove Room segment rows (postings cascade). Old `vector_index/hnsw.idx` is ignored; app re-indexes via WorkManager.
- **Files changed:** `app/di/VectorModule.kt`, `core/vector/segmented/SegmentedVectorIndex.kt` (`clear()` manifest cleanup)
- **Tests/checks:** `./gradlew :app:compileDebugKotlin`, `./gradlew assembleDebug`
- **Branch:** `main`

---

### Phase 13 - Performance Agent - adaptive search config and batch embedding
- **Summary:** Added `AdaptiveSearchConfig` for battery/thermal/index-size-aware `efSearch`. Refactored `EmbeddingWorker` to batch vector inserts via `addBatch()` before `persist()`. Verified existing CLIP ImageNet normalization in `ImagePreprocessor`; added unit test.
- **Files changed:** `core/vector/AdaptiveSearchConfig.kt`, `AdaptiveSearchConfigTest.kt`, `core/worker/EmbeddingWorker.kt`, `core/ml/ImagePreprocessorClipTest.kt`, `docs/WORK_LOG.md`
- **Tests/checks:** `./gradlew assembleDebug`, `:core:vector:testDebugUnitTest --tests "*.AdaptiveSearchConfig*"`, `:core:worker:compileDebugKotlin`, `:core:ml:testDebugUnitTest`

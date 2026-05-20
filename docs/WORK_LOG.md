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

# Recall — Project State

**Last updated:** Phase 0 (Project Setup Agent)  
**Status:** Multi-module Gradle structure initialized; build verified.

## Architecture

```
:app
├── :core:common          — shared utilities, dispatchers
├── :core:database        — Room persistence
├── :core:media           — media loading (Coil)
├── :core:ml              — TensorFlow Lite inference
├── :core:vector          — vector search utilities
├── :core:worker          — WorkManager background jobs
├── :core:designsystem    — Compose theme & components
├── :feature:search
├── :feature:timeline
├── :feature:detail
├── :feature:settings
└── :feature:onboarding
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

## Convention Plugins (`build-logic/`)

- `recall.android.library` — Android library defaults
- `recall.android.feature` — feature module (library + compose + hilt + nav deps)
- `recall.android.application` — app module defaults
- `recall.android.compose` — Compose BOM and UI dependencies
- `recall.hilt` — Hilt + KSP wiring

## Phase Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Discovery & repository setup | **Complete** |
| 1 | Core database & models | Pending |
| 2 | ML pipeline | Pending |
| 3 | Feature screens | Pending |
| 4 | Background indexing | Pending |
| 5 | Integration & polish | Pending |

## Next Steps

1. Define Room entities and DAOs in `:core:database`
2. Implement ML embedding pipeline in `:core:ml`
3. Build feature navigation graph in `:app`
4. Add WorkManager indexing workers in `:core:worker`

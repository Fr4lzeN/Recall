# Recall — Work Log

## Phase 0 — Project Setup Agent

**Date:** 2026-05-20  
**Agent:** Project Setup Agent

### Completed

- [x] Git repository initialized with `.gitignore` (ML models, `.cxx/`, build dirs)
- [x] `build-logic/` convention plugins created
- [x] `gradle/libs.versions.toml` populated with verified dependency versions
- [x] Multi-module structure: 7 core + 5 feature modules
- [x] Package renamed `com.example.recall` → `com.recall.app`
- [x] `:app` wired to all modules via convention plugins
- [x] `RecallDispatchers` placeholder in `:core:common`
- [x] `./gradlew assembleDebug` build verification

### Version Verification (May 2026)

| Dependency | Version Used | Notes |
|------------|--------------|-------|
| KSP | 2.3.7 | AGP 9 built-in Kotlin compatible |
| Hilt | 2.59.2 | AGP 9 compatible |
| Room | 2.8.4 | Latest stable 2.x |
| TFLite | 2.17.0 | `tensorflow-lite-support` deferred (LiteRT manifest conflict) |
| Navigation | 2.9.8 | Latest stable |
| WorkManager | 2.11.2 | Latest stable |
| Coil | 3.4.0 | Coil 3 stable |

### Issues Encountered

1. **AGP 9 `CommonExtension` generics removed** — Convention plugins use `LibraryExtension` / `ApplicationExtension` directly.
2. **AGP 9 built-in Kotlin** — Removed `org.jetbrains.kotlin.android` from convention plugins (conflicts with built-in `kotlin` extension).
3. **KSP + built-in Kotlin** — Upgraded KSP from `2.2.10-2.0.2` to `2.3.7` for AGP 9 source-set registration.
4. **TensorFlow Lite versions** — `2.18.0` does not exist; use `2.17.0`. `tensorflow-lite-support` uses separate version `0.5.0` but causes LiteRT manifest namespace collision — omitted from `:core:ml` until resolved.
5. **Configuration cache warning** — Non-fatal serialization warning on `processDebugNavigationResources`; build succeeds.

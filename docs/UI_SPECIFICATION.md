# Recall — UI Specification for Figma AI

> **Version:** 1.0 · **Platform:** Android (phone) · **Toolkit:** Jetpack Compose + Material 3  
> **Purpose:** Pixel-accurate mockup generation for Recall — a privacy-first, on-device semantic photo/video search app. All ML indexing and search run locally; no network required.

---

## App Overview

| Attribute | Value |
|-----------|-------|
| **App name** | Recall |
| **Tagline (internal)** | Your personal photo memory assistant |
| **Platform** | Android 8+ (API 26+ target per architecture); UI spec targets modern phones (360–412dp width) |
| **Default theme** | Dark-first (`RecallTheme(darkTheme = true)`) |
| **Design system** | Material 3 (M3) with custom `RecallTheme`, `RecallTypography`, `RecallShapes` |
| **Display mode** | Edge-to-edge; status bar and navigation bar scrim `#121212`, dark icons |
| **Privacy positioning** | Copy emphasizes on-device AI; photos never leave the device |

### Design language

- **Aesthetic:** Smart minimalism — deep charcoal surfaces, warm amber primary actions, violet secondary accents, generous whitespace in empty states.
- **Density:** Comfortable; grids are tight (4dp gaps) but touch targets meet 48dp minimum via `minimumInteractiveComponentSize()`.
- **Motion:** 300ms transitions; subtle empty-state icon pulse (1.4s loop); pull-to-refresh on Timeline; detail screen slides up from bottom.
- **Icon style:** Material Icons — **Filled** for bottom navigation and search-field leading icon; **Outlined** for settings list items, empty states, and onboarding.

---

## Color Palette

### Dark theme (default — use for all primary mockups)

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#121212` | Screen background, system bar scrim |
| `surface` | `#0D0D0D` | Top app bar, bottom nav bar, cards |
| `primary` | `#F5A623` | CTAs, progress indicators, match-score badges, section headers, focused borders |
| `onPrimary` | `#1A1A1A` | Text/icons on primary-filled surfaces |
| `secondary` | `#7B61FF` | Secondary accents (future chips, links) |
| `onSecondary` | `#FFFFFF` | Text on secondary |
| `tertiary` | `#E8A87C` | “Indexing” badges on timeline tiles |
| `onBackground` | `#E8E8E8` | Primary text on background |
| `onSurface` | `#E0E0E0` | Primary text on surfaces |
| `onSurfaceVariant` | `#B0B0B0` | Secondary text, placeholders, empty-state icons (60% opacity) |
| `outline` | `#3D3D3D` | Unfocused text field borders, dividers base |
| `outlineVariant` | ~`#3D3D3D` at lower emphasis | `HorizontalDivider` in Settings |
| `error` | `#CF6679` | Error text, error-state icon |
| `onError` | `#1A1A1A` | Text on error containers |

**Material 3 derived (dynamic / scheme defaults — approximate for mockups):**

| Token | Suggested hex (dark) | Usage |
|-------|---------------------|-------|
| `surfaceVariant` @ 40% alpha | `#2A2A2A` @ 40% | Grid tile placeholder behind images |
| `surfaceContainerHigh` | `#2C2C2C` | Media detail metadata panel |
| `primaryContainer` | `#3D2E10` | “Indexed” badge background |
| `onPrimaryContainer` | `#F5D9A0` | “Indexed” badge text |
| `tertiaryContainer` | `#3D2A1F` | “Not indexed” badge background |
| `onTertiaryContainer` | `#F0D4C0` | “Not indexed” badge text |
| `scrim` @ 70% alpha | `#000000` @ 70% | Video duration pill background |

### Light theme (fallback — secondary artboard)

| Token | Hex |
|-------|-----|
| `background` | `#F5F5F5` |
| `surface` | `#FFFBFE` |
| `primary` | `#E09400` |
| `onPrimary` | `#FFFFFF` |
| `secondary` | `#5B45D6` |
| `tertiary` | `#8B6914` |
| `onBackground` / `onSurface` | `#1C1B1F` |
| `onSurfaceVariant` | `#49454F` |
| `outline` | `#79747E` |
| `error` | `#B3261E` |

> On Android 12+, dynamic color may tint secondary roles; **always preserve** `surface` `#0D0D0D` and `background` `#121212` in dark mode per `RecallTheme`.

---

## Typography Scale

**Font family:** System default (Roboto on most devices). All styles from `RecallTypography` (Material 3 type scale).

| Style | Size / Line height | Weight | Letter spacing | Primary use |
|-------|-------------------|--------|----------------|-------------|
| `displayLarge` | 57 / 64 sp | Normal | −0.25 sp | — |
| `displayMedium` | 45 / 52 sp | Normal | — | — |
| `displaySmall` | 36 / 44 sp | Normal | — | — |
| `headlineLarge` | 32 / 40 sp | SemiBold | — | — |
| `headlineMedium` | 28 / 36 sp | SemiBold | — | Onboarding title |
| `headlineSmall` | 24 / 32 sp | SemiBold | — | — |
| `titleLarge` | 22 / 28 sp | Medium | — | Top app bar titles, empty/error titles |
| `titleMedium` | 16 / 24 sp | Medium | 0.15 sp | Timeline date headers |
| `titleSmall` | 14 / 20 sp | Medium | 0.1 sp | Settings section headers |
| `bodyLarge` | 16 / 24 sp | Normal | 0.5 sp | Onboarding body, search placeholder |
| `bodyMedium` | 14 / 20 sp | Normal | 0.25 sp | Empty/error descriptions, metadata values |
| `labelLarge` | 14 / 20 sp | Medium | 0.1 sp | Buttons |
| `labelMedium` | 12 / 16 sp | Medium | 0.5 sp | Indexing status badge (detail) |
| `labelSmall` | 11 / 16 sp | Medium | 0.5 sp | Match %, video duration, timeline indexing chip |

---

## Shape & Elevation

| M3 shape | Corner radius |
|----------|---------------|
| `extraSmall` | 4 dp |
| `small` | 8 dp — grid tiles, metadata badge |
| `medium` | 12 dp |
| `large` | 16 dp — search field outline |
| `extraLarge` | 28 dp |

| Surface | Elevation / tonal |
|---------|-------------------|
| Bottom navigation | 0 dp (flat `surface`) |
| Top app bar | 0 dp (flat `surface`) |
| Media detail metadata panel | `tonalElevation = 2 dp` on `surfaceContainerHigh` |
| Alert dialogs | M3 default (elevation ~3 dp equivalent) |

---

## Global Components

### RecallTopBar

- **Component:** M3 `CenterAlignedTopAppBar`
- **Height:** 64 dp (M3 default)
- **Background:** `surface` `#0D0D0D`
- **Title:** `titleLarge`, `onSurface`, centered
- **Navigation slot:** Optional back `IconButton` (48×48 dp touch target), `Icons.AutoMirrored.Filled.ArrowBack`, content description “Navigate back”
- **Actions slot:** End-aligned custom content (e.g. indexing label on Timeline)

### RecallSearchBar

- **Component:** M3 `OutlinedTextField`, `fillMaxWidth`, `singleLine`
- **Shape:** `large` (16 dp corners)
- **Label:** “Search query” (floating label, M3 behavior)
- **Placeholder:** “Search your memories…” — `bodyLarge`, `onSurfaceVariant`
- **Leading icon:** `Icons.Filled.Search`, 24 dp, content description “Search”
- **Trailing icon:** Clear button when query non-empty — `Icons.Filled.Clear`, “Clear search”
- **Border:** focused → `primary` `#F5A623`; unfocused → `outline` `#3D3D3D`
- **Screen padding:** 16 dp horizontal, 12 dp vertical (around field)
- **IME:** Search action on keyboard

### EmptyState

- **Layout:** `Column`, `fillMaxSize`, centered, **32 dp** padding all sides
- **Icon:** 64×64 dp, `onSurfaceVariant` @ **60%** opacity; optional pulse scale 0.92↔1.08 over **1400 ms** (`FastOutSlowInEasing`, reverse repeat) when `animateIcon = true`
- **Title:** `titleLarge`, `onSurface`, center, **16 dp** below icon
- **Description:** `bodyMedium`, `onSurfaceVariant`, center, **8 dp** below title
- **Semantics:** Combined content description `"$title. $description"`

### ErrorState

- Same layout/spacing as EmptyState
- **Icon:** `Icons.Outlined.ErrorOutline`, 64 dp, `error` `#CF6679`
- **Primary button:** “Try again” (customizable `retryLabel`), **24 dp** below description
- **Button:** M3 filled `Button`, `labelLarge`

### LoadingState

- **Layout:** `Box` fillMaxSize, centered
- **Indicator:** M3 `CircularProgressIndicator`, color `primary` `#F5A623`, default 40 dp diameter

### Media grid tile (shared pattern)

- **Aspect ratio:** 1:1
- **Shape:** `small` (8 dp)
- **Background:** `surfaceVariant` @ **40%** alpha (visible while image loads)
- **Image:** `ContentScale.Crop`, fill tile
- **Touch target:** `minimumInteractiveComponentSize()` → **48 dp** minimum
- **Grid:** `GridCells.Adaptive(minSize = 120 dp)` → ~3 columns on 360 dp width
- **Content padding:** 8 dp
- **Gaps:** 4 dp horizontal and vertical

### Bottom NavigationBar

- **Visibility:** Shown only on top-level routes: `search`, `timeline`, `settings`. Hidden on `onboarding`, `detail/{mediaId}`, `directory-exclusions` (new).
- **Background:** `surface` `#0D0D0D`
- **Height:** 80 dp (M3 default including labels)
- **Insets:** `NavigationBarDefaults.windowInsets` (edge-to-edge safe area)
- **Items (3):**

| Tab | Label | Icon |
|-----|-------|------|
| Search | “Search” | `Icons.Filled.Search` |
| Timeline | “Timeline” | `Icons.Filled.History` |
| Settings | “Settings” | `Icons.Filled.Settings` |

- **Selected state:** M3 `NavigationBarItem` default — active indicator pill, `primary` tint on icon/label
- **Touch target:** 48 dp per item

---

## Navigation Structure

### Route map

```
onboarding          → first launch / permissions not granted
search              → main tab (default after onboarding)
timeline            → tab
settings            → tab
detail/{mediaId}    → pushed from search or timeline
directory-exclusions → pushed from settings (NEW — design target)
```

### Flow diagram (text)

```
[App Launch]
    │
    ├─ shouldShowOnboarding? ──YES──► [Onboarding Screen]
    │                                      │
    │                                      ├─ Grant permissions ──► [Search] (pop onboarding)
    │                                      └─ Denied permanently ──► System Settings
    │
    └─ NO ──► [Search] (start destination)

[Bottom Nav: Search | Timeline | Settings]
    │
    ├─ Tap grid item (Search/Timeline) ──► [Media Detail] (slide up + fade, 300ms)
    │       └─ Back ──► previous tab
    │
    └─ Settings ──► "Manage Excluded Directories" ──► [Directory Exclusion] (slide horizontal, 300ms)
            └─ Back ──► Settings
```

### Screen transitions (300 ms, `tween`)

| From → To | Enter | Exit |
|-----------|-------|------|
| Tab ↔ Tab | Fade in | Fade out |
| Tab → Detail | Slide up from bottom + fade in | Forward slide (tab exits left) |
| Detail → Tab | Fade in (tab) | Slide down + fade out |
| Any → Onboarding | Slide in from right + fade | Slide out left + fade |
| Settings → Directory Exclusion | Slide in from right + fade | Slide out left + fade |

### Scaffold hierarchy

```
MainActivity (edge-to-edge)
└── RecallTheme
    └── Scaffold (bottomBar = NavigationBar when top-level)
        └── RecallNavHost (padding from Scaffold)
            └── Feature Scaffold per screen (optional topBar)
```

---

## Screen 1: Onboarding / Permission

### Purpose

First-run experience: explain privacy value proposition, request `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`, handle denial and permanent denial. Completing onboarding navigates to Search and removes onboarding from back stack.

### Layout (top to bottom)

1. **Safe area:** Full screen; content vertically centered in a `Column` with **32 dp** horizontal padding.
2. **Hero icon** — centered
3. **24 dp** spacer
4. **Title** — “Welcome to Recall”
5. **12 dp** spacer
6. **Body copy** — centered paragraph
7. **32 dp** spacer
8. **Contextual block** — permission CTA or error state (varies by state)

No top app bar. No bottom navigation.

### UI Elements

| Element | Spec |
|---------|------|
| Lock icon | `Icons.Outlined.Lock`, **72 dp** height (width intrinsic), tint `primary` `#F5A623`, decorative (no content description) |
| Title | “Welcome to Recall”, `headlineMedium`, `onBackground` `#E8E8E8`, center |
| Description | “Your personal photo memory assistant. Recall searches your library using on-device AI — your photos never leave your phone.”, `bodyLarge`, `onSurfaceVariant`, center |
| Primary button | M3 `Button`, full intrinsic width, label “Grant Access” / “Continue” / “Open Settings” |
| Secondary button | M3 `OutlinedButton`, “I've granted access” (permanent denial path only), **8 dp** below primary |
| Error text | “Media access was denied. Open Settings to grant permission.”, `bodyMedium`, `error` `#CF6679`, center |

### Rationale dialog (overlay)

- **Component:** M3 `AlertDialog`
- **Icon:** `Icons.Outlined.Lock`, 24 dp
- **Title:** “Media access needed” — `headlineSmall`
- **Body:** “Recall needs access to your photos and videos to search and organize your memories. All processing stays on your device.”
- **Confirm:** `TextButton` “Grant access”
- **Dismiss:** `TextButton` “Not now”

### States

| State | UI |
|-------|-----|
| `NOT_REQUESTED` | Description + “Grant Access” button; may show rationale dialog first if OS says so |
| `GRANTED` | “Continue” button → completes onboarding |
| `DENIED` | Rationale dialog may auto-show with “Grant access” / “Not now” |
| `PERMANENTLY_DENIED` | Error text + “Open Settings” (primary) + “I've granted access” (outlined) |

### Interactions

| Action | Result |
|--------|--------|
| Tap “Grant Access” | System permission sheet (or rationale dialog first) |
| Permissions granted | `onOnboardingComplete()` → navigate to Search, pop onboarding |
| Tap “Open Settings” | Android app-details settings intent |
| Tap “I've granted access” | Re-check permissions; if granted, complete onboarding |
| Tap “Not now” (dialog) | Dismiss dialog |

### Animations

- Dialog: M3 standard scale + fade
- No custom screen transition on first paint (onboarding is start destination)

### Accessibility

- Buttons: minimum 48 dp touch height
- Dialog title/body read in order
- Error state announced when `PERMANENTLY_DENIED`

---

## Screen 2: Search (Main Tab)

### Purpose

Primary entry: natural-language semantic search over indexed media. Shows indexing progress in empty state, debounced query (**300 ms**), top-50 results with similarity scores.

### Layout (top to bottom)

1. **Scaffold** — no top bar; respects parent bottom nav + edge insets
2. **RecallSearchBar** — full width with 16/12 dp padding
3. **Content area** — one of: Error / Empty / Loading / Results grid

### UI Elements

| Element | Spec |
|---------|------|
| Search field | See RecallSearchBar; bound to query string |
| Results grid | Adaptive min cell **120 dp**, 8 dp padding, 4 dp gaps |
| Result tile | 1:1 `Surface` (clickable), image crop, score badge |
| Score badge | Top-end, **6 dp** inset; background `primary` @ **90%**; padding H6 V2; text `"{N}%"` `labelSmall` `onPrimary`; corner `extraSmall` (4 dp) |
| Semantics | Button role; description `"{displayName}, {score}% match"` |

### States

| Condition | UI |
|-----------|-----|
| `error != null` | ErrorState: title “Search failed”, description = error message, “Try again” |
| `query.isBlank()` + `indexedCount == 0` + `totalCount > 0` | EmptyState: “Indexing not started” / “Your photos haven't been indexed yet. Go to Settings to start.” / `PhotoLibrary` icon |
| `query.isBlank()` + otherwise | EmptyState: “Search your memories” / dynamic count description / `Search` icon |
| `query` not blank + `indexedCount == 0` | EmptyState: indexing not started (same as above) |
| `isSearching` | LoadingState (centered spinner) |
| Results empty | EmptyState: “No photos match your search” / “Try a different description — Recall matches photos by meaning, not exact filenames.” / `ImageNotSupported`, **no** icon animation |
| Results populated | Grid of tiles |

**Indexed count description (blank query):**

- If `totalCount > 0`: “`{indexedCount}` of `{totalCount}` photos indexed. Describe a photo or moment — Recall will find matching images in your library.”
- Else: “Describe a photo or moment — Recall will find matching images once your library is indexed.”

### Interactions

| Action | Result |
|--------|--------|
| Type in search field | Updates query; blank clears results; non-blank triggers debounced search |
| Tap clear (trailing) | Clears query |
| Tap result tile | Navigate to `detail/{mediaId}` |
| Tap “Try again” | Re-runs search for current query |
| Keyboard Search | `onSearch()` callback (optional; same pipeline as typing) |

### Animations

- Empty state icon pulse (default on)
- Grid items: no shared-element transition in current code (optional design enhancement)
- Tab switch: 300 ms fade

### Accessibility

- Search field: labeled “Search query”; leading/trailing icons described
- Each result: percent match in content description
- Empty/error: combined semantics on EmptyState

---

## Screen 3: Timeline (Tab)

### Purpose

Chronological browse of all media in the library, grouped by date. Shows indexing progress in app bar and per-tile badges. Pull-to-refresh triggers full re-index pipeline.

### Layout (top to bottom)

1. **RecallTopBar** — title “Timeline”; optional action text when indexing
2. **PullToRefreshBox** — wraps entire content
3. **LinearProgressIndicator** — full width, when scanning (conditional)
4. **Content** — Empty or date-grouped grid

### UI Elements

| Element | Spec |
|---------|------|
| Top bar title | “Timeline”, `titleLarge` |
| Indexing action | “Indexing `{indexed}`/`{total}`”, `labelSmall`, `primary`, end padding **16 dp**; visible when `indexedCount < mediaCount && mediaCount > 0` |
| Progress bar | M3 indeterminate `LinearProgressIndicator`, full width, height 4 dp; when `mediaCount == 0 && pipeline active` |
| Date header | Full grid span; `titleMedium`, `onSurface`; padding **4 dp** horizontal, **8 dp** vertical |
| Media tile | Same 1:1 grid as Search; video overlays below |
| Video play icon | `Icons.Filled.PlayArrow`, center, `onSurface` @ 85%, bottom padding **16 dp** on icon |
| Duration pill | Bottom-end, **6 dp** inset; `scrim` @ 70%; `labelSmall` text `onPrimary`; format `M:SS` |
| Indexing chip | Top-start, **6 dp** inset; `tertiary` @ 90%; text “Indexing”, `labelSmall` `onTertiary`; when `!isIndexed` |

**Date format:** `EEEE, MMM d, yyyy` (e.g. “Thursday, May 21, 2026”)

### States

| Condition | UI |
|-----------|-----|
| Scanning (`mediaCount == 0` + pipeline) | Progress bar + EmptyState “Scanning your gallery…” / “We're finding photos and videos on your device. This may take a moment.” |
| No media, not scanning | EmptyState “No photos found” / “Grant access in Settings to see your library here.” / `Settings` icon, no animation |
| Has media | LazyVerticalGrid with interleaved headers and tiles |

### Interactions

| Action | Result |
|--------|--------|
| Pull down | `refresh()` → starts full indexing; `isRefreshing` true while pipeline active |
| Tap tile | Navigate to detail |
| Bottom nav | Switch tabs (fade) |

### Animations

- Pull-to-refresh: M3 default indicator (primary color)
- Progress bar: indeterminate sweep
- Empty state icon pulse on scanning state

### Accessibility

- Date headers: not explicitly headings in code — **design recommendation:** add heading semantics
- Tiles: `"{displayName}, video"` / `", indexing"` suffix in description
- Video indicator icon: “Video indicator”

---

## Screen 4: Media Detail

### Purpose

Full preview of a single photo or video with file metadata and indexing status. Entry from Search or Timeline.

### Layout (top to bottom)

1. **RecallTopBar** — title = `displayName` or “Media”; back button start
2. **Column**
   - **MediaPreview** — `weight(1f)`, fill width
   - **MediaMetadataPanel** — bottom sheet–style panel (fixed height by content, not draggable in code)

### UI Elements

| Element | Spec |
|---------|------|
| Back button | 48 dp touch target, `ArrowBack` mirrored |
| Preview area | Background `surface`; image `ContentScale.Fit` (letterbox); fill available height |
| Video overlay | `PlayArrow` centered, **32 dp** padding, `onSurface` @ 70% |
| Metadata panel | `surfaceContainerHigh` ~`#2C2C2C`, tonal elevation **2 dp**, width fill |
| Panel padding | **16 dp** all sides |
| Row spacing | **8 dp** vertical between metadata rows |
| Indexing badge | Pill: H10 V4 padding; `small` corners; “Indexed” (`primaryContainer`) or “Not indexed” (`tertiaryContainer`); `labelMedium` |
| Metadata row | Label: `labelSmall` `onSurfaceVariant`; Value: `bodyMedium` `onSurface` |
| Fields | Filename, Date (`MMM d, yyyy 'at' h:mm a`), Dimensions (`W × H`), Size (B/KB/MB/GB), Type (mime), Duration (video only, `M:SS`) |

### States

| Condition | UI |
|-----------|-----|
| `mediaItem == null` | Centered `CircularProgressIndicator` (loading) |
| Loaded | Preview + metadata |

### Interactions

| Action | Result |
|--------|--------|
| Tap back | `popBackStack()` — detail exits with slide down 300 ms |
| Tap preview | No action in current code (design: optional pinch-zoom future) |

### Animations

- Enter: slide up from bottom + fade (300 ms)
- Exit (pop): slide down + fade

### Accessibility

- Preview: “Photo preview: {name}” or “Video preview: {name}”
- Back: “Navigate back”
- Badge and rows read in document order

---

## Screen 5: Settings (Tab)

### Purpose

Indexing status, ML model profile, device capabilities, storage, destructive actions (re-index, clear index), about. **Entry point for Directory Exclusion (new).**

### Layout (top to bottom)

1. **RecallTopBar** — “Settings”
2. **LazyColumn** — scrollable sections

### Section: Indexing Status

| Element | Spec |
|---------|------|
| Section header | “Indexing Status”, `titleSmall`, `primary`, padding **16×12 dp**, heading semantics |
| ListItem | Leading: `Icons.Outlined.Sync` “Indexing status”; Headline: “`{indexed}` of `{total}` items indexed”; Supporting: progress + status text |
| Progress | `LinearProgressIndicator` full width; indeterminate when `isIndexing`, else determinate `indexed/total` |
| Status text | “Indexing in progress…” / “Scan your library to begin indexing” / “`{percent}`% complete” |

### Section: Model Profile

| Row | Headline | Supporting |
|-----|----------|------------|
| Profile | Active profile name | “`{dimensions}` dimensions · `{QUANTIZATION}`” |
| Model file | “Model file” | Model file name |
| Icons | `Tune`, `SmartToy` | |

### Section: Device Info

| Row | Content |
|-----|---------|
| RAM | “`{available}` MB available / `{total}` MB total” — `Memory` icon |
| CPU cores | Count — `DeveloperBoard` |
| Android API | “API `{version}`” — `PhoneAndroid` |
| NNAPI | “Supported” / “Not supported” — `Android` |

### Section: Storage

| Row | Content |
|-----|---------|
| Free disk | “`X.X` GB free” or “`N` MB free” — `SdStorage` |

### Section: Indexing Scope (NEW)

| Element | Spec |
|---------|------|
| Section header | “Indexing Scope”, `titleSmall`, `primary` |
| ListItem (nav row) | Leading: `Icons.Outlined.FolderOff` (or `Folder` with slash badge); Headline: “Manage Excluded Directories”; Supporting: “`{excludedCount}` folders excluded · `{skippedItems}` items skipped” (dynamic); Trailing: `Icons.Outlined.ChevronRight` 24 dp; **min height 72 dp** |
| Tap | Navigate to Directory Exclusion screen |

> **Implementation note:** Supporting text shows live counts from persisted exclusion prefs; when none excluded: “No folders excluded — all media will be indexed.”

### Section: Actions

| Element | Spec |
|---------|------|
| Container padding | **16 dp** horizontal, **8 dp** vertical |
| Button spacing | **8 dp** between buttons |
| Re-index All | M3 `Button` fillMaxWidth, `Refresh` icon + label; **disabled** when `isIndexing` |
| Clear Index | M3 `OutlinedButton` fillMaxWidth, `DeleteSweep` icon; **disabled** when indexing or `indexedMedia == 0` |

### Section: About

| Row | “Recall” / “Version `{versionName}`” — `Info` icon |

### Dividers

- `HorizontalDivider` between sections: color `outlineVariant`, margin **16 dp** horizontal, **4 dp** vertical

### Dialogs

**Re-index confirmation**

- Title: “Re-index all photos?”
- Body: “This will re-scan and re-embed your entire library. It may take a while and use battery while running.”
- Confirm: “Re-index” (`TextButton` style in code)
- Cancel: “Cancel”

**Clear index confirmation**

- Title: “Clear search index?”
- Body: “This removes all embeddings from the index. Your photos stay on device, but search won't work until indexing runs again.”
- Confirm: “Clear”
- Cancel: “Cancel”

### States

| Condition | UI |
|-----------|-----|
| Indexing active | Indeterminate progress; buttons disabled |
| Zero indexed | Clear Index disabled |
| Normal | Determinate progress bar |

### Interactions

| Action | Result |
|--------|--------|
| Tap “Manage Excluded Directories” | Push Directory Exclusion screen |
| Tap Re-index All | Show dialog → `reindexAll()` pipeline |
| Tap Clear Index | Show dialog → clear vector index + mark items unindexed |
| Scroll | Standard LazyColumn |

### Animations

- Tab fade on enter
- Dialog standard M3

### Accessibility

- Section headers: `heading()` semantics
- ListItem icons: content descriptions per row
- Progress: announce percent when updated (recommendation)

---

## Screen 6: Directory Exclusion (NEW)

### Purpose

Let users exclude specific media folders from indexing (DCIM, Pictures, Downloads, app-specific folders, etc.). Exclusions apply on the **next** indexing run; offer re-index after changes.

### Layout (top to bottom)

1. **RecallTopBar** — title “Excluded Folders”; back navigation
2. **Summary card** — pinned below top bar, always visible
3. **LazyColumn** — folder rows
4. **Bottom sticky bar (optional)** — primary CTA when dirty state

### Summary card

| Property | Value |
|----------|-------|
| Container | M3 `ElevatedCard` or `Surface` with `medium` (12 dp) corners |
| Margin | **16 dp** horizontal, **12 dp** top, **8 dp** bottom |
| Padding | **16 dp** internal |
| Background | `surfaceContainerHigh` or `primaryContainer` @ 30% if any exclusions |
| Icon | `Icons.Outlined.FolderOff`, 24 dp, `primary` |
| Headline | “`{excludedFolderCount}` folders excluded” — `titleMedium` |
| Supporting | “`{skippedItemCount}` items will be skipped on next index” — `bodyMedium` `onSurfaceVariant` |
| Footnote | “Changes take effect on the next indexing run.” — `bodySmall`, `onSurfaceVariant` @ 80% |
| Zero state copy | “No folders excluded — your entire library will be indexed.” |

### Folder row (repeat)

| Property | Value |
|----------|-------|
| Component | M3 `ListItem` or custom row, **min height 80 dp** |
| Leading | **56×56 dp** rounded thumbnail (`small` 8 dp corners) — 2×2 collage of latest media in folder, or `Icons.Outlined.Folder` fallback on `surfaceVariant` |
| Headline | Folder display name (e.g. “DCIM”, “Pictures/Screenshots”) — `bodyLarge` |
| Supporting line 1 | Relative path (e.g. `/storage/emulated/0/DCIM/Camera`) — `bodySmall` `onSurfaceVariant`, max 1 line ellipsis |
| Supporting line 2 | “`{itemCount}` photos & videos” — `labelMedium` |
| Trailing | M3 `Switch` — **ON** = included in index, **OFF** = excluded |
| Excluded visual | When switch OFF: row background `error` @ 8% or left border **4 dp** `error` `#CF6679`; strikethrough optional on path text; `FolderOff` badge overlay on thumbnail @ 50% scrim |

**Suggested default folders (examples for mockups):**

| Folder | Item count | Default |
|--------|------------|---------|
| DCIM/Camera | 1,842 | Included (ON) |
| Pictures/Screenshots | 326 | Included |
| Downloads | 89 | Excluded (OFF) |
| WhatsApp/Media | 412 | Excluded |
| Pictures/Recall | 12 | Included |

### Sticky bottom action (when `hasUnsavedChanges` or after toggle)

| Element | Spec |
|---------|------|
| Bar background | `surface` with top shadow elevation 8 dp |
| Padding | **16 dp**, safe nav inset |
| Primary button | “Apply & Re-index” — M3 `Button` fillMaxWidth, `Refresh` icon |
| Secondary | `TextButton` “Apply without re-indexing” — saves prefs only |

**Confirmation dialog (re-index path)**

- Title: “Re-index with new exclusions?”
- Body: “Excluded folders will be removed from the search index. This may take a while.”
- Confirm: “Re-index” / Cancel: “Later”

### States

| State | UI |
|-------|-----|
| Loading folders | Centered `CircularProgressIndicator` + “Reading folders…” |
| Empty device | EmptyState: “No media folders found” / “Grant media access to manage folders.” |
| Populated | Summary + list |
| All included | Summary shows zero exclusions; no error tint on rows |
| Search/filter (v2) | Optional top `SearchBar` — not required for v1 mockup |

### Interactions

| Action | Result |
|--------|--------|
| Toggle switch | Mark folder excluded/included in local UI state; update summary counts live |
| Tap row (non-switch area) | Optional: expand to show 4 thumbnail previews in horizontal strip |
| Tap back | If dirty, confirm discard dialog; else pop to Settings |
| Tap “Apply & Re-index” | Persist exclusions → start `startFullIndexing()` → pop to Settings |
| Tap “Apply without re-indexing” | Persist only; show snackbar “Exclusions saved. Re-index from Settings when ready.” |

### Animations

- Switch: M3 standard thumb slide
- Row exclusion tint: 200 ms `animateColorAsState`
- Enter screen: horizontal slide 300 ms (match onboarding forward)

### Accessibility

- Switch: “Include `{folderName}` in indexing” / state on/off
- Summary: heading “Exclusion summary”
- Thumbnail: “Preview of `{folderName}`”
- Minimum 48 dp touch on switch and row

---

## Cross-Cutting Design Guidelines

### Edge-to-edge

- `enableEdgeToEdge()` in `MainActivity`; transparent status/nav bars; dark scrim `#121212`
- Scaffold and `NavigationBar` use `windowInsets` — content must not draw under gesture nav without padding
- Top app bars: default M3 inset handling

### Touch targets

- Minimum **48×48 dp** for all icon buttons, nav items, grid cells (via `minimumInteractiveComponentSize()`)
- Switch on directory row: 48 dp height row minimum

### Content descriptions (required patterns)

| Component | Pattern |
|-----------|---------|
| Search result | `"{name}, {percent}% match"` |
| Timeline item | `"{name}"` + `", video"` + `", indexing"` |
| Nav icons | Tab label as description |
| Empty/error | Combined title + description |
| Settings icons | Per-row labels (“Indexing status”, “RAM”, etc.) |

### Bottom sheet pattern (design guidance)

- Media detail metadata panel behaves like a **fixed bottom panel** (not draggable). Future: draggable `ModalBottomSheet` for “Similar photos” section with `secondary` accent chips.

### Pull-to-refresh

- **Timeline only** in current implementation
- Design: primary-colored M3 indicator

### Snackbars (recommended, not in code)

- Directory exclusions saved
- Re-index started
- Index cleared

---

## Design Deliverables Checklist for Figma AI

1. **Artboard:** Phone 360×800 dp (or 412×915), dark theme default  
2. **Frames:** Onboarding, Search (5 states), Timeline (4 states), Detail, Settings, Directory Exclusion (3 states), Component sheet (TopBar, SearchBar, Empty/Error/Loading, Grid tile, Nav bar)  
3. **Variables:** Color tokens table above; spacing 4/8/12/16/32 dp scale  
4. **Prototypes:** Tab fade; Detail slide up; Settings → Directory Exclusion push; Toggle exclusion tint  
5. **NEW feature:** Settings “Indexing Scope” section + full Directory Exclusion screen with summary card and switch rows  

---

## Appendix: Implementation vs Design Gaps

| Item | Status |
|------|--------|
| Directory Exclusion screen | **Design-only (this spec)** — not in `RecallNavHost` yet |
| Settings “Manage Excluded Directories” row | **Design-only** |
| Similarity / “Similar photos” on detail | Not in current `MediaDetailScreen` — omit or future frame |
| Search screen top bar | Intentionally absent — search bar is hero |
| Light theme artboard | Optional secondary; app defaults dark |

---

*Generated from Recall codebase: `SearchScreen`, `TimelineScreen`, `MediaDetailScreen`, `SettingsScreen`, `OnboardingScreen`, `core:designsystem` components and theme, `RecallApp` navigation. Directory Exclusion per product requirement.*

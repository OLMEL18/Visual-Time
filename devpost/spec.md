---
doc: spec
status: approved
---

# Visual Time — Technical Specification

## How This Works, In Plain Language

Visual Time is one native Android app with one activity and one Compose screen. That screen changes between setup, running, paused, and finished without navigating to separate pages. A small `ViewModel` holds the current UI and timer session, while focused helper classes handle timer arithmetic, duration formatting, input validation, and recent-duration storage.

The timer does not advance by counting animation frames. When a session starts, the app records the phone's monotonic elapsed-time clock. Every visible refresh asks, “How much real elapsed time has passed?” and derives both the remaining time and a normalized progress value. The hourglass Canvas receives only that progress value and whether sand should be flowing. Its cosmetic motion may be smooth or imperfect without making the countdown early or late.

The only persisted product data is an ordered list of up to three recent durations. Preferences DataStore stores that list locally. A missing preference is different from an existing empty preference: only a missing value is seeded with 15, 25, and 40. The typed manual draft and any active timer remain in memory only and disappear after process death.

This shape is intentionally small: no navigation framework, database, dependency-injection framework, service, backend, game engine, or grain-by-grain simulation. It keeps the distinctive hourglass and low-friction timer behavior at the center of the proof of concept while leaving the pure timer logic reusable by a future Dial renderer.

## The Core Journey Through the System

Implements `prd.md > The Core Journey` and `prd.md > Features and Behavior`.

1. `MainActivity` opens edge-to-edge and shows `VisualTimeScreen`. The stationary hourglass is immediately visible; the manual field is not focused and the keyboard stays closed.
2. `VisualTimeViewModel` asks `RecentDurationsRepository` for the stored ordered list. In one DataStore transaction, the repository seeds `[15, 25, 40]` only when the preference key is absent. An existing empty value remains empty.
3. A quick-start tap sends a start event directly. Manual Start or IME Done first goes through `DurationInputValidator`; invalid text stays in setup with inline feedback, while a valid integer dismisses the keyboard and starts the same path as quick start.
4. Starting a session applies `RecentDurationsPolicy`, immediately moves the selected duration to the front without duplication, trims to three, and asks the repository to persist the order. A quick start does not alter the separate manual draft.
5. `TimerEngine` creates an immutable session anchored to `MonotonicClock.nowMillis()`. The selected original duration fixes the numeric format for the whole session. The ViewModel changes to Running, hides the setup panel, and starts with the overlay hidden.
6. While the activity is visible and the session is running, a lifecycle-aware pulse requests fresh timer snapshots. Each snapshot supplies accurate remaining milliseconds and elapsed progress. `HourglassCanvas` renders the corresponding upper pile, lower pile, and fine stream; cosmetic stream movement never feeds back into the clock.
7. A background tap reveals `TimerOverlay`. Its time comes from `DurationFormatter`; any positive fractional second rounds up, and zero is emitted only when `TimerEngine` has reached expiry. A monotonic five-second deadline hides a running overlay after inactivity.
8. Pause records the exact accumulated elapsed time and removes the running anchor. Progress and stream both freeze. Resume creates a new anchor from the same frozen point. Restart creates a fresh session for the original duration; Back clears the session and returns to setup.
9. When the app leaves the foreground, its UI pulse stops. The session is not logically paused. On a live-process return, the next monotonic snapshot catches up immediately or changes the phase to Finished. A paused session remains frozen.
10. At expiry, the final progress is exactly `1.0`, the stream is off, and `FinishedPanel` shows Finished, the correct fixed-format zero, Repeat, and Back to setup. Repeat follows the normal start path for the same duration.

## Stack

### Project and Platform Identity

- Display name: **Visual Time**.
- `applicationId`: `com.kisss.visualtime`.
- Kotlin namespace and all source packages: `com.kisss.visualtime`.
- One application module named `app`, one `ComponentActivity`, and one Compose surface.
- `minSdk = 26`, `compileSdk = 37`, and `targetSdk = 36`.
- The phone activity requests portrait orientation. The layout still uses bounded constraints and safe insets so it remains crash-safe if Android ignores that request on a large display. No orientation-request workaround or large-screen opt-out is added.

Implements `prd.md > Screens and Layout` and `prd.md > Product Decisions`.

### Build Toolchain

The approved toolchain is:

| Part | Version / rule | Reason |
|---|---|---|
| Android Gradle Plugin | `9.2.1` | Stable requested version; supports the installed JDK and API range. |
| Gradle wrapper | `9.4.1` | AGP 9.2's documented compatible Gradle version. |
| JDK | `17` | AGP 9.2's required/default JDK and already installed locally. |
| Kotlin | AGP 9.2 built-in Kotlin, KGP runtime `2.2.10` | Avoids applying the legacy `org.jetbrains.kotlin.android` / `kotlin-android` plugin beside built-in Kotlin. |
| Compose compiler plugin | `org.jetbrains.kotlin.plugin.compose` version `2.2.10` | Matches the Kotlin toolchain; the Compose BOM does not select the compiler. |
| Compose libraries | Stable BOM `2026.09.00` | Keeps Compose library versions mutually compatible. |
| SDK levels | `minSdk 26`, `compileSdk 37`, `targetSdk 36` | Android 8 minimum, API 37 compilation, and Android 16 target behavior. |

All explicit plugin and non-BOM library versions live in `gradle/libs.versions.toml`. Compose artifacts managed by the BOM omit individual versions. The app module applies `com.android.application` and `org.jetbrains.kotlin.plugin.compose`; it must not apply `org.jetbrains.kotlin.android` or `kotlin-android`.

Official references:

- [AGP 9.2 compatibility and release notes](https://developer.android.com/build/releases/agp-9-2-0-release-notes)
- [AGP 9 built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)

#### Resolved SDK and Compose Compatibility

The official BOM mapping associates BOM `2026.09.00` with Compose UI/Foundation `1.12.1`, while current Android documentation states that Compose `1.12.0` and later require `compileSdk 37`. The approved coherent configuration therefore uses Android SDK Platform 37 and `compileSdk 37` with BOM `2026.09.00`.

`minSdk 26` still permits installation on Android 8.0 and later, including the authorized Redmi Note 9 Pro running Android 12/API 31. `targetSdk 36` remains the runtime compatibility contract. Raising only `compileSdk` makes API 37 symbols and compatible library metadata available to the build; it neither raises the minimum supported Android version nor opts the app into target SDK 37 behavior.

Android SDK Platform 37 is not yet installed in the inspected environment. Installing/verifying it is the first toolchain preparation action in `5-build`, before project scaffolding or application implementation.

The first Gradle smoke test must resolve dependencies, confirm AGP built-in Kotlin plus the matching Compose Compiler plugin without the legacy Kotlin Android plugin, and complete a real `assembleDebug`. If Gradle reports a dependency, Kotlin, Compose Compiler, or metadata mismatch, capture the exact error and propose the smallest stable compatible correction before changing versions. There will be no silent downgrade, alpha/experimental dependency, dependency-metadata override, or forced incompatible build. See [Compose setup and compile-SDK compatibility](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) and the [official BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping).

### Runtime Libraries

The minimal intended dependency set is:

- Compose UI, Foundation, Runtime, Material 3, tooling preview, and Compose UI testing, all governed by stable Compose BOM `2026.09.00`.
- `androidx.activity:activity-compose` for the single Compose activity and edge-to-edge entry point.
- `androidx.lifecycle:lifecycle-viewmodel-compose` and `lifecycle-runtime-compose` for the ViewModel and lifecycle-aware visible updates.
- `androidx.datastore:datastore-preferences:1.2.1` for the ordered recent-duration value.
- JUnit 4 plus Kotlin coroutines test support for deterministic local tests; AndroidX/Compose test dependencies only for the small interaction suite.

Supporting AndroidX versions must be stable, explicitly pinned in the version catalog, compatible with the final compile SDK, and proven by the first sync/build. No Navigation, Hilt, Room, WorkManager, game engine, image library, network client, analytics SDK, or third-party animation library is included.

References: [AndroidX stable releases](https://developer.android.com/jetpack/androidx/versions/stable-channel), [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore), and [Compose testing](https://developer.android.com/develop/ui/compose/testing).

## Where It Runs and How Someone Tries It

Visual Time runs entirely on an Android phone and requires no account, API key, network access, server, or Play Store publication.

The inspected development environment is Windows with Android Studio Panda 4 / 2025.3.4, JDK 17, Android SDK at `K:\Android\Sdk`, currently installed platforms 36/36.1, Build Tools 36.0.0 or later, ADB, and the Android emulator. SDK Platform 37 is required and will be installed first in `5-build`. No AVD is currently configured, and emulator setup is not on the critical path.

The primary device is already authorized over ADB:

- Xiaomi **Redmi Note 9 Pro** as reported by ADB
- Android 12 / API 31
- Serial `[redacted-device-serial]`

After `4-spec` approval and project creation in `5-build`, the exact local path is the repository root. The expected commands are:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
K:\Android\Sdk\platform-tools\adb.exe devices -l
K:\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
K:\Android\Sdk\platform-tools\adb.exe shell am start -n com.kisss.visualtime/.MainActivity
```

Targeted instrumented tests may be run with:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The first phone installation happens at the minimal Canvas prototype milestone, before the complete setup and timer controls are polished. The final hackathon demonstration is recorded from the physical phone and accompanied by a public GitHub source repository. The local learner profile remains excluded from that repository by the root `.gitignore` rule `/devpost/learner-profile.md`; verify the rule with Git after repository initialization and before the first public push. Google Play publication and a reviewer-installable production release are not required for this POC.

## Look and Feel

Implements `prd.md > Look and Feel`, `prd.md > Screens and Layout`, and `scope.md > Inspiration & Identity`.

- Render edge-to-edge on true black `#000000`; keep status and navigation surfaces visually quiet with appropriate light system-bar icons.
- Begin with warm sand around `#E2B76B`, subtly translucent cool-gray glass, and restrained dark-metal or warm-neutral supports. These are fixed POC theme values, not user settings.
- Use the Android/system sans-serif family. The remaining-time style uses tabular numerals and a fixed container wide enough for `24:00:00`, preventing horizontal movement as digits change.
- Use compact rounded charcoal/translucent panels, subtle outlines, light but non-glaring text, visible labels, and touch targets of at least roughly 48 dp.
- Keep the hourglass in a stable layout layer. The top quick-start panel, bottom manual-entry panel, overlay, pause indicator, and finished controls are drawn in separate overlay layers and must never measure or reposition it.
- The hourglass bounds are derived from stable window constraints and capped on wider displays. IME insets are not applied to its geometry, so opening the keyboard cannot resize or recenter it under `adjustNothing`.
- Draw the hourglass procedurally: symmetric curved glass paths, fine contours and restrained reflection, clipped nonrectangular sand piles, and one continuous-looking narrow stream. Avoid dashed lines, stacked repeated strokes, rectangular chamber fills, heavy glow, celebratory motion, or particle-physics objects.
- A small deterministic cosmetic phase may vary the stream and surface texture while flowing. That phase stops when paused/finished and never determines timer progress.

## Components

### Main Activity and Window Policy

`MainActivity` is the only activity. It enables edge-to-edge drawing, uses transparent/black system-bar treatment, hosts the Compose tree, and connects activity lifecycle visibility to the ViewModel's UI pulse. The manifest sets `android:windowSoftInputMode="adjustNothing"` and requests portrait for the phone activity.

The Compose content applies safe drawing/cutout top and horizontal insets to the top quick-start panel, and navigation-bar bottom insets to the bottom manual-entry panel and timer controls. IME insets apply only to the bottom manual-entry panel so it clears the keyboard; they never resize or reposition the hourglass or top panel. The system IME gets the first opportunity to consume Android Back; screen-level Back behavior then follows the timer state.

Implements `prd.md > Screens and Layout` and `prd.md > Background, Relaunch, and System Back`.

References: [edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge), [Compose insets](https://developer.android.com/develop/ui/compose/system/insets), and [Android 16 orientation behavior](https://developer.android.com/about/versions/16/behavior-changes-16#adaptive-layouts).

### Visual Time Screen

`VisualTimeScreen` is a full-window `Box` with three independent layers: the stable `HourglassCanvas`, phase-specific panels/overlays, and whole-background tap handling. It selects setup, running/paused, or finished content from one immutable `VisualTimeUiState`; no navigation library or screen back stack exists.

Control elements consume their own taps, so Pause, Resume, Restart, Back, Repeat, Start, and quick starts never also trigger the background show/hide action.

Implements `prd.md > Screens and Layout`, `prd.md > Starting a Session`, and `prd.md > Overlay and Pause`.

### Visual Time ViewModel

`VisualTimeViewModel` is the single coordinator. It exposes immutable `StateFlow<VisualTimeUiState>` and accepts named user/lifecycle events. It owns the process-local manual draft, validation message, current timer session, UI phase, overlay visibility/deadline, and the latest recent-duration list received from the repository.

It delegates rather than embedding specialized rules: validation to `DurationInputValidator`, time calculations to `TimerEngine`, formatting to `DurationFormatter`, and persistence to `RecentDurationsRepository`. Back, Restart, Repeat, pause/resume, and completion are state transitions here. Returning to setup clears the session and all active pulse/timeout work but preserves the in-memory manual draft.

No `SavedStateHandle` persistence is used for the active session or manual input. ViewModel survival covers ordinary activity recreation/live process behavior; process-death relaunch intentionally starts a new setup state.

Implements `prd.md > Features and Behavior` and `prd.md > States and Boundaries`.

Reference: [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel).

### Timer Engine and Monotonic Clock

`MonotonicClock` is a tiny interface returning elapsed milliseconds. Production uses `SystemClock.elapsedRealtime()`; tests use a manually advanced fake. `TimerEngine` is pure Kotlin: given a session and a clock value, it returns remaining milliseconds, normalized elapsed progress from `0.0` to `1.0`, and whether expiry has occurred.

A running session stores its original duration, elapsed time accumulated before the current run, and its current monotonic start anchor. Pause folds the current interval into accumulated elapsed time and removes the anchor. Resume creates a new anchor; Restart creates a clean session; Back discards it. Wall-clock changes and animation frame rates cannot alter these calculations.

Implements `prd.md > Sand Progress and Time Display`, `prd.md > Restart, Back, and Completion`, and `prd.md > Background, Relaunch, and System Back`.

Reference: [`SystemClock.elapsedRealtime()`](https://developer.android.com/reference/android/os/SystemClock#elapsedRealtime()).

### Lifecycle-Aware Update Pulse

The Compose/activity lifecycle runs the UI refresh pulse only while lifecycle state is at least Started and the timer phase is Running. Entering the foreground performs an immediate snapshot before continuing periodic visible updates. Leaving the foreground cancels the pulse but leaves the session's monotonic anchor unchanged.

Paused, setup, finished, and background states have no continuous timer refresh. A separate cosmetic stream phase also runs only when visible and flowing. The ViewModel makes the transition to Finished from an authoritative timer snapshot; the Canvas never decides expiry.

Implements `prd.md > Background, Relaunch, and System Back` and `scope.md > Technical Risks`.

### Duration Input Validator

`DurationInputValidator` treats an empty field as neutral while editing and validates only on Start/Done or when deciding whether to clear an existing error. It safely parses arbitrary pasted text without converting through a type that can overflow. Only a base-10 whole integer from 1 through 1,440 is accepted; whitespace policy is consistent, while signs, decimals, letters, oversized values, and overflow-prone values are rejected without throwing.

The setup field requests the standard Android numeric keyboard and IME Done. Valid Start and Done use the same event; invalid submission produces the inline “Enter 1–1440 minutes” message.

Implements `prd.md > Manual Duration and Validation`.

### Duration Formatter

`DurationFormatter` selects Short (`MM:SS`) or Long (`HH:MM:SS`) once from the original duration. It computes displayed seconds as the ceiling of positive remaining milliseconds divided by 1,000. It returns zero only when the timer snapshot says the session has actually expired and preserves the original format through Finished.

Implements `prd.md > Sand Progress and Time Display` and `prd.md > Finished State`.

### Recent Durations Policy and Repository

`RecentDurationsPolicy` is a pure function: prepend the started duration, remove duplicates while preserving order, and keep the first three. `RecentDurationsRepository` persists the ordered result with Preferences DataStore.

Because a `Set<String>` would not preserve MRU order, one string preference stores a compact ordered comma-separated value. The repository checks key presence separately: absent means seed and store `15,25,40`; present empty string means an intentionally empty list and is never seeded. Reading defensively ignores malformed/out-of-range values, removes duplicates, and caps the list at three. If a present value sanitizes to empty, it remains an existing empty history rather than becoming presets.

Starting any session updates the in-memory list immediately and performs one DataStore edit. Typing does nothing to storage. Quick-start use leaves the manual draft untouched. Persistence is local and independent from session recovery.

Implements `prd.md > Quick Starts and Recent Durations` and `prd.md > Background, Relaunch, and System Back`.

Reference: [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore).

### Setup Panels

`SetupQuickStartPanel` renders the title and zero to three quick-start buttons in a compact top safe area. Its row tolerates an intentionally empty history without placeholder buttons. `ManualDurationPanel` separately renders the manual field, Start action, and inline validation in a compact bottom safe area. Neither panel requests input focus at creation or return.

Both panels are positioned independently of the hourglass. With `adjustNothing`, the top panel and hourglass stay stationary; only the bottom manual-entry panel uses Compose IME insets to remain visible above the keyboard. This Xiaomi-dependent behavior is a mandatory physical-device checkpoint.

Implements `prd.md > Setup State`, `prd.md > Quick Starts and Recent Durations`, and `prd.md > Manual Duration and Validation`.

### Timer Overlay and Pause Indicator

`TimerOverlay` draws the fixed-width time near the top and labelled Pause/Resume, Restart, and Back controls in a bottom safe area. Running visibility uses a monotonic five-second inactivity deadline; control interaction resets or cancels that deadline as appropriate. A background tap hides it immediately.

Paused state keeps the overlay indefinitely by default. If explicitly dismissed, `PauseIndicator` leaves a small unobtrusive marker while the hourglass remains frozen. Another background tap restores the paused controls. Nested controls consume taps before the outer background handler.

Implements `prd.md > Running and Paused States` and `prd.md > Overlay and Pause`.

### Finished Panel

`FinishedPanel` is persistent and never uses the overlay timeout. It shows Finished, zero in the session's fixed time format, and labelled Repeat and Back to setup actions in the stable bottom control area. Repeat immediately follows the normal start/MRU path; Back clears the session.

Implements `prd.md > Finished State` and `prd.md > Restart, Back, and Completion`.

### Hourglass Canvas and Geometry

`HourglassCanvas(elapsedProgress, isFlowing)` owns only presentation. `elapsedProgress = 0.0` means full upper/empty lower; `1.0` means empty upper/full lower. It clamps unexpected inputs and draws into stable bounds supplied by the screen.

`HourglassGeometry` builds reusable normalized paths for the silhouette, inner chamber clips, glass contour/reflections, frame, upper sand mass, lower mound, and neck stream. Sand surfaces use curved/angled pile contours and clipping rather than rectangular fills. A lightweight deterministic cosmetic phase may vary a narrow continuous stream and a few fine accents while `isFlowing` is true; there are no independently simulated grain objects.

The exact silhouette, pile functions, and stream treatment are deliberately validated in the early physical-device prototype before the rest of the UI is polished.

Implements `prd.md > Look and Feel`, `prd.md > Sand Progress and Time Display`, and `scope.md > The Unique Kernel`.

Reference: [Compose Canvas graphics](https://developer.android.com/develop/ui/compose/graphics/draw/overview).

### Theme and Text Resources

The Compose theme defines the black surface, warm-sand accent, restrained glass/frame/text colors, shapes, spacing, and system-sans typography. User-facing text stays in Android string resources. The numeric timer style uses tabular features and a preallocated long-format width.

Implements `prd.md > Look and Feel`.

## Data Model

### In-Memory UI Data

Conceptual immutable state:

| Value | Owner | Update rule | After background / process death |
|---|---|---|---|
| `phase` (`Setup`, `Running`, `Paused`, `Finished`) | ViewModel | User actions and authoritative timer snapshots | Preserved in a live process; resets to Setup after process death. |
| `manualDraft: String` | ViewModel | Manual field edits only | Preserved in a live process; empty after process death. |
| `validationMessage` | ViewModel | Invalid submit; cleared when corrected or session starts | Process-local. |
| `recentDurations: List<Int>` | Repository flow mirrored in ViewModel | Session start applies MRU policy | Persisted across relaunch. |
| `session: TimerSession?` | ViewModel / TimerEngine | Start, pause, resume, restart, repeat, back, expiry | Preserved only while process lives; never restored after death. |
| `overlayVisible` | ViewModel | Background taps, controls, state transitions | Process-local. |
| `overlayHideDeadlineElapsedMs` | ViewModel | Running overlay show/interaction | Re-evaluated from monotonic time; absent while paused/finished. |

`TimerSession` contains the original duration in milliseconds, accumulated elapsed milliseconds, optional running anchor from the monotonic clock, and the fixed Short/Long display format. Derived remaining time and progress are snapshots, not separately persisted authorities.

### Persisted Data

Preferences DataStore contains one ordered recent-duration key:

```text
recent_durations_ordered = "15,25,40"
```

- Missing key: write defaults once and emit `[15, 25, 40]`.
- Present empty string: emit `[]`; never reseed.
- Present valid list: parse, validate, de-duplicate in first-seen order, and cap at three.
- Timer start: write the updated list in one DataStore edit.

There is no database schema, active-session record, manual-draft preference, account, telemetry, or network cache.

## System Back and Input Dispatch

Implements `prd.md > Manual Duration and Validation` and `prd.md > Background, Relaunch, and System Back`.

Back is resolved in this order:

1. A visible system keyboard dismisses without submission or cancellation.
2. In Running or Paused with the overlay hidden, Back reveals it.
3. In Running or Paused with the overlay visible, Back cancels and returns to setup.
4. In Finished, Back returns to setup.
5. In Setup with no keyboard, normal Activity/system behavior may leave the app; no hidden timer exists.

IME Done routes to the same validated start event as the Start button. Background taps and control taps are separate event targets, preventing propagation into overlay dismissal.

## Build and Verification Sequence

### 1. SDK Platform and Toolchain Smoke Test

At the start of `5-build`, install or verify Android SDK Platform 37 before creating application code. Then create the minimal project configuration, resolve dependencies, inspect the Kotlin/Compose Compiler plugin configuration, and run the valid wrapper command `.\gradlew.bat assembleDebug` to completion before feature code grows. Confirm that AGP built-in Kotlin is active, only the matching Compose Compiler plugin is added, the legacy Kotlin Android plugin is absent, and dependencies are stable and minimal. If resolution or compilation fails, preserve and report the exact error and obtain agreement on the smallest stable compatible correction; do not silently downgrade, bypass dependency metadata, or enable experimental versions.

### 2. Early Hourglass Prototype and Phone Checkpoint

Create the smallest runnable screen with the true-black background and `HourglassCanvas`. Give the prototype a controllable/sample progress value solely for visual development, build a debug APK, and install it immediately on the authorized Redmi Note 9 Pro. Review silhouette, glass restraint, natural upper/lower pile shapes, fine continuous stream, perceived smoothness, and obvious CPU/runaway-animation behavior.

This prototype is a build-order checkpoint, not a substitute for the accurate timer. Do not proceed to full screen polish if the distinctive visual kernel is plainly below the agreed threshold; iterate the Canvas first or discuss the concrete tradeoff.

### 3. Timer Core and State Transitions

Implement the clock abstraction, timer engine, fixed formatter, ViewModel phases, lifecycle-aware pulse, pause/resume/restart/back/repeat, overlay deadlines, and deterministic unit tests. Prove that background time catches up without coupling countdown accuracy to rendering.

### 4. Setup, Validation, and Persistence

Implement DataStore absence-aware seeding, MRU policy, process-local manual draft, safe manual validation, quick starts, standard numeric IME, setup fade, and input tests.

### 5. Complete Overlays and Finished State

Add final control surfaces, system Back rules, pause indicator, finished presentation, safe insets, fixed numeric layout, and polished theme while preserving the hourglass geometry established at the checkpoint.

### 6. Targeted Verification

Run deterministic unit tests and the small Compose interaction suite, then install the current debug APK on the phone. Manually verify keyboard visibility, Done, Start, keyboard-first Back, `adjustNothing`, stable top panel/hourglass geometry, bottom entry-panel IME clearance, overlay tap isolation, five-second hiding, pause dismissal, session Back behavior, live-process foreground return, expiry, and performance. Emulator checks are optional and must not delay the phone demo.

## Testing Plan

### Deterministic Unit Tests

- Timer arithmetic at start, mid-session, pause, resume, restart, exact expiry, over-expiry, and foreground catch-up using `FakeMonotonicClock`.
- Display rounding where 1–999 positive milliseconds shows one second, and zero appears only at actual expiry.
- Fixed Short/Long formatting at 59 minutes, 60 minutes, 90 minutes, 720 minutes, and 1,440 minutes, including finished zero.
- Input validation for boundaries 1/1,440, empty text, pasted valid integers, whitespace policy, zero, negatives, signs, fractional values, mixed characters, Unicode/unusual text, and extremely large overflow-prone strings.
- MRU ordering, uniqueness, trimming, reuse, empty history, and quick-start/manual-start equivalence.
- Repository behavior for absent key, present empty value, valid stored value, and malformed stored value.
- ViewModel transitions for start, pause/resume, restart, Back, repeat, finish, overlay timeout, and live-process foreground return.

### High-Value Compose Tests

- Setup displays current quick starts and valid quick start immediately enters clean Running.
- Invalid manual submission stays in setup with inline feedback; valid Start/Done follows the common start path.
- Background tap reveals/hides controls, control clicks do not propagate, and pause keeps or explicitly dismisses its overlay as specified.
- Finished shows the correct fixed-format zero with Repeat and Back.

The suite intentionally excludes screenshot/golden infrastructure, broad automated device matrices, and battery benchmarks.

### Physical-Device Checklist

The Redmi Note 9 Pro/API 31 is authoritative for the POC checkpoint. Inspect visual quality by eye and exercise actual Xiaomi keyboard/window behavior. In particular, confirm that `adjustNothing` keeps the top quick-start panel and hourglass geometry stationary while the bottom manual-entry panel clears the keyboard and keeps its input and Start action visible. If it does not, record the observed movement/occlusion and propose the smallest platform-compatible adjustment before changing the UX; do not silently add a custom keypad.

## File Structure

```text
Visual-Time/
├── settings.gradle.kts                         # Repositories and the single app module.
├── build.gradle.kts                            # Root AGP/Compose plugin aliases only.
├── gradle.properties                           # Small, standard Android/Gradle settings.
├── gradle/
│   ├── libs.versions.toml                      # All explicit plugin/library versions and aliases.
│   └── wrapper/gradle-wrapper.properties       # Pins Gradle 9.4.1.
├── app/
│   ├── build.gradle.kts                        # SDK levels, namespace, Compose, dependencies, tests.
│   ├── proguard-rules.pro                      # Empty/minimal future release rules; debug is primary.
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml             # App identity, single portrait Activity, adjustNothing.
│       │   ├── java/com/kisss/visualtime/
│       │   │   ├── MainActivity.kt             # Edge-to-edge host and lifecycle connection.
│       │   │   ├── VisualTimeViewModel.kt      # UI/session coordinator and event handling.
│       │   │   ├── VisualTimeViewModelFactory.kt # Manual construction; no DI framework.
│       │   │   ├── VisualTimeUiState.kt        # Immutable phases and UI-facing data.
│       │   │   ├── data/
│       │   │   │   ├── RecentDurationsPolicy.kt     # Pure unique three-item MRU operation.
│       │   │   │   └── RecentDurationsRepository.kt # Ordered DataStore encoding/seeding.
│       │   │   ├── timer/
│       │   │   │   ├── MonotonicClock.kt       # Production interface plus SystemClock adapter.
│       │   │   │   ├── TimerEngine.kt          # Pure elapsed/remaining/progress calculations.
│       │   │   │   ├── TimerSession.kt         # Immutable session anchor and fixed format.
│       │   │   │   ├── DurationFormatter.kt    # Ceiling seconds and stable time strings.
│       │   │   │   └── DurationInputValidator.kt # Safe 1–1,440 parsing.
│       │   │   └── ui/
│       │   │       ├── VisualTimeScreen.kt     # Stable layered single-screen composition.
│       │   │       ├── SetupPanel.kt           # Quick starts, input, Start, validation.
│       │   │       ├── TimerOverlay.kt         # Time and running/paused controls.
│       │   │       ├── FinishedPanel.kt        # Finished, zero, Repeat, Back.
│       │   │       ├── PauseIndicator.kt       # Minimal hidden-overlay paused marker.
│       │   │       ├── hourglass/
│       │   │       │   ├── HourglassCanvas.kt  # Draws from progress and flowing flag.
│       │   │       │   └── HourglassGeometry.kt # Normalized paths and pile geometry.
│       │   │       └── theme/
│       │   │           ├── Color.kt             # Black, sand, glass, frame, text palette.
│       │   │           ├── Theme.kt             # Compose theme and system-bar tone.
│       │   │           └── Type.kt              # System sans and tabular timer style.
│       │   └── res/
│       │       ├── values/strings.xml           # User-visible labels and validation text.
│       │       ├── values/themes.xml            # Black launch/window theme.
│       │       └── mipmap-*/                    # Standard generated launcher icon resources.
│       ├── test/java/com/kisss/visualtime/
│       │   ├── timer/TimerEngineTest.kt
│       │   ├── timer/DurationFormatterTest.kt
│       │   ├── timer/DurationInputValidatorTest.kt
│       │   ├── data/RecentDurationsPolicyTest.kt
│       │   ├── data/RecentDurationsRepositoryTest.kt
│       │   └── VisualTimeViewModelTest.kt
│       └── androidTest/java/com/kisss/visualtime/
│           └── VisualTimeFlowTest.kt            # Small Compose interaction suite.
├── devpost/
│   ├── learner-profile.md                       # Local-only profile; excluded by root .gitignore.
│   ├── scope.md                                 # Approved POC scope.
│   ├── prd.md                                   # Approved product behavior.
│   └── spec.md                                  # This technical blueprint.
├── .gitignore
└── README.md                                    # Build, install, test, and demo instructions.
```

## External Services and Dependencies

There are no external services, endpoints, accounts, API keys, rate limits, hosting costs, network permissions, or runtime internet requirements.

The only external build inputs are the Android SDK, JDK, Gradle/AGP, Kotlin/Compose compiler plugin, stable AndroidX libraries, and test libraries listed under `Stack`. Gradle needs network access once to resolve artifacts unless they are already cached; the installed app then runs offline.

## Important Failure Modes

- **SDK 37, dependency, Kotlin, or Compose Compiler resolution fails** → preserve the exact Gradle error and stop at the toolchain smoke test. Propose the smallest stable compatible correction; never silently downgrade, bypass metadata, add the legacy Kotlin Android plugin, or use experimental versions.
- **The sand reads as a fill animation rather than a physical quantity** → iterate the procedural silhouette, clipped curved piles, and stream during the early phone checkpoint before building the remaining UI. If the threshold still threatens the deadline, present the observed tradeoff before changing an agreed feature.
- **Xiaomi IME behavior moves the top panel/hourglass or lets the keyboard obscure the bottom input/Start despite `adjustNothing` plus panel-scoped IME insets** → capture the exact on-device behavior and propose the smallest inset/window-policy adjustment. Do not introduce a custom keypad or change the approved interaction without agreement.
- **Stored recent-duration text is malformed** → sanitize to valid unique 1–1,440 values without crashing. A present value that becomes empty remains empty and is not reseeded.

## What Was Simplified and Why

- **One Compose activity and one state-driven surface** instead of navigation among screens — all POC states share the same stable hourglass and do not need a navigation graph.
- **A pure timer engine plus one ViewModel** instead of a generalized multi-mode/domain architecture — enough separation for a later Dial renderer without building abstractions for a mode that does not exist yet.
- **Preferences DataStore with one ordered string** instead of Room or a database — only three small integers need persistence, and order/absence semantics remain explicit.
- **Procedural visual illusion** instead of grain-level physics or a game engine — the kernel is believable visual depletion, not simulated particles.
- **Live-process monotonic correctness** instead of a foreground service, notifications, or process-death session recovery — matches the approved lifecycle boundary and avoids background-platform scope.
- **Standard Android numeric IME** instead of a custom keypad — preserves accessibility and input behavior while keeping the POC focused.
- **Physical-phone visual and interaction checks** instead of broad device automation and golden screenshots — the connected demo phone is the most relevant evidence for this hackathon.

## Decisions and Open Issues

### Learner-Approved Decisions

- Native Kotlin, Jetpack Compose, custom Canvas, one Activity, one small ViewModel, and Preferences DataStore.
- Permanent identity `com.kisss.visualtime` and display name Visual Time.
- `minSdk 26`, `compileSdk 37`, `targetSdk 36`, SDK Platform 37, AGP 9.2.1, Gradle 9.4.1, JDK 17, built-in Kotlin, Compose BOM 2026.09.00, stable dependencies, and a version catalog.
- Raising `compileSdk` to 37 neither raises the Android 8.0 minimum nor opts the app into target SDK 37 behavior; the Redmi Note 9 Pro on Android 12/API 31 remains the primary device.
- No legacy Kotlin Android plugin beside AGP built-in Kotlin; use the Compose Compiler Gradle plugin matched to the Kotlin toolchain.
- Injectable `SystemClock.elapsedRealtime()` abstraction and pure timer math; rendering progress never controls clock progress.
- Edge-to-edge layout, safe interactive insets, `adjustNothing`, no launch autofocus, and portrait phone activity without large-screen orientation hacks.
- Deterministic unit tests, a small Compose interaction suite, and early installation of the Canvas prototype on the physical phone.

### Useful Unknowns and Agreed Investigations

- **Visual sand quality is the largest genuine unknown.** Requirements describe the target, but the convincing balance of silhouette, mound geometry, stream width, texture, and performance cannot be settled on paper. The agreed investigation is the early minimal debug APK on the Redmi Note 9 Pro, followed by focused Canvas iteration before full UI polish.
- **`adjustNothing` behavior on the Xiaomi device is platform/IME dependent.** The agreed investigation verifies keyboard opening, IME Done, Start, Back dismissal, a stationary top panel and invariant hourglass bounds, plus bottom manual-panel clearance on the actual phone. Any fallback is discussed from observed evidence rather than assumed now.

### Resolved Compatibility Decision

- **SDK/Compose compatibility:** use SDK Platform and `compileSdk 37` with Compose BOM `2026.09.00`, while retaining `minSdk 26` and `targetSdk 36`. Installation and an actual dependency-resolution/`assembleDebug` proof are the first `5-build` toolchain checkpoint.

No other consequential product or architecture questions remain. Application implementation must not begin while this document is `status: draft`.

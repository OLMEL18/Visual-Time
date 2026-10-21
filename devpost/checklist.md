---
doc: checklist
status: approved
---

# Visual Time — Build Checklist

Build mode: fast

## Slices

- [ ] **1. The approved Android shell builds cleanly**
  Becomes usable: A minimal Visual Time debug APK is produced locally with the permanent `com.kisss.visualtime` identity, true-black edge-to-edge surface, portrait phone request, and the approved stable toolchain. This is the critical-risk exception; the first functional/visual checkpoint remains slice 2.
  Why now: SDK 37 and the AGP built-in Kotlin/Compose Compiler combination must be proven before feature code grows. A real successful build exposes dependency metadata or plugin incompatibility while correction is still cheap.
  PRD ref: `prd.md > Screens and Layout`, `prd.md > What We're Building`
  Spec ref: `spec.md > Stack`, `spec.md > Where It Runs and How Someone Tries It`, `spec.md > Main Activity and Window Policy`, `spec.md > File Structure`, `spec.md > Build and Verification Sequence > 1. SDK Platform and Toolchain Smoke Test`
  Build: Install or verify Android SDK Platform 37. Scaffold the single-module Gradle project and minimal one-Activity Compose shell from the annotated file tree using AGP 9.2.1, Gradle 9.4.1, JDK 17, `minSdk 26`, `compileSdk 37`, `targetSdk 36`, stable Compose BOM 2026.09.00, AGP built-in Kotlin, and the matching Compose Compiler plugin without the legacy Kotlin Android plugin. Keep dependencies minimal. After the smoke test passes, inspect GitHub authentication/account information and ask for explicit owner/remote approval before creating a private `Visual-Time` repository or changing anything remotely. Once approved, create it as private, connect `origin`, make and push a safe planning/ignore-rules initial commit, then commit and push the verified scaffold separately. Before the first push and every commit, run `git check-ignore -v devpost/learner-profile.md`, inspect status and the staged diff, and exclude credentials, local properties, generated output, and unrelated files. Keep the remote private throughout development; changing it to public is a separate learner-approved `6-ship` action after the final privacy/content audit.
  Verify (mechanical): Confirm SDK Platform 37 is installed; inspect the effective plugins and dependency graph; run `.\gradlew.bat assembleDebug` and require a real successful APK. On any Kotlin, Compose Compiler, dependency, or metadata error, preserve the exact output and stop for learner agreement before changing approved versions. Confirm `git check-ignore -v devpost/learner-profile.md` names the root rule, `local.properties` is ignored, no secrets/private profile are tracked or staged, and the intended commits/push succeed after remote approval.
  Learner check: Review the reported effective toolchain and successful APK path, then confirm the GitHub owner and private-repository action when asked. No phone visual judgment is requested yet.
  Commit: `Bootstrap verified Android build` (after the initial safe `Document approved Visual Time plan` repository commit)

- [ ] **2. A convincing hourglass prototype runs on the phone**
  Becomes usable: The debug APK shows a large, stable custom Canvas hourglass on true black with warm nonrectangular sand piles and a fine continuous-looking stream. A clearly development-only looping/sample progress driver makes the full sand transition inspectable; it is replaced by the real timer in slice 3.
  Why now: The hourglass is the unique kernel and the largest uncertainty. Reviewing it on the actual display before building all controls prevents polishing a timer around an unconvincing visual.
  PRD ref: `prd.md > Look and Feel`, `prd.md > Screens and Layout > Setup State`, `prd.md > Sand Progress and Time Display`
  Spec ref: `spec.md > Look and Feel`, `spec.md > Hourglass Canvas and Geometry`, `spec.md > Build and Verification Sequence > 2. Early Hourglass Prototype and Phone Checkpoint`, `spec.md > Important Failure Modes`
  Build: Implement normalized hourglass geometry, cool-gray glass contours/reflection, restrained frame, curved/clipped upper and lower sand masses, and a lightweight cosmetic stream animation that runs only while flowing. Keep the hourglass bounds stable and independent of overlays/IME. Use only a development/test-only progress driver for this checkpoint; it must be replaced by the real timer in slice 3 and must not become a permanent demo-speed control. Build, install, and launch the APK on the authorized Redmi Note 9 Pro; do not add timer controls, Dial, settings, or grain physics.
  Verify (mechanical): Run `.\gradlew.bat assembleDebug`; install with ADB; launch `com.kisss.visualtime/.MainActivity`; confirm the process stays alive without crash/error logs; capture and inspect representative device states at approximately 0%, 25%, 50%, 75%, and 100% progress. Check redistribution, clipping/geometry artifacts, abrupt pile-shape changes, and that the stream appears only while progress is actively flowing. Confirm the visual driver stops work when not flowing and does not control authoritative timer state.
  Learner check: Pause after slice 2. On the Redmi Note 9 Pro, inspect approximately 0%, 25%, 50%, 75%, and 100% progress and review silhouette, glass restraint, natural redistribution, pile continuity, clipping/artifacts, stream visibility/fineness, smoothness, scale, and calm character. Resolve visual feedback before timer/UI expansion; this check does not introduce particle physics or new features.
  Commit: `Prototype flowing hourglass Canvas`

- [ ] **3. Quick starts run an accurate visual timer to completion**
  Becomes usable: The setup surface offers 15, 25, and 40 quick starts; tapping one immediately enters the clean hourglass countdown, sand follows monotonic elapsed time, and expiry shows the final lower pile with Finished, fixed-format zero, Repeat, and Back.
  Why now: This replaces the visual prototype driver with the product's real kernel—time becoming a shrinking physical quantity—before adding input and control complexity.
  PRD ref: `prd.md > The Core Journey` (steps 1–6 and 10), `prd.md > Starting a Session`, `prd.md > Sand Progress and Time Display`, `prd.md > Restart, Back, and Completion`
  Spec ref: `spec.md > Visual Time ViewModel`, `spec.md > Timer Engine and Monotonic Clock`, `spec.md > Lifecycle-Aware Update Pulse`, `spec.md > Duration Formatter`, `spec.md > Finished Panel`, `spec.md > Data Model`
  Build: Add immutable setup/running/finished UI state, the injectable monotonic clock, pure timer session math, fixed session-based time format, lifecycle-aware visible pulse, quick-start actions, setup fade, authoritative completion, Repeat, and Back. Feed only normalized elapsed progress plus flowing state into the Canvas; cosmetic frame timing remains separate.
  Verify (mechanical): Add and run deterministic unit tests for start/midpoint/exact expiry/over-expiry, positive-fraction ceiling, fixed short/long formats through zero, restart/repeat/back transitions, and progress clamping. Run `.\gradlew.bat testDebugUnitTest` and `.\gradlew.bat assembleDebug`; install/launch on the phone and confirm quick start enters a number-free running view without crashes.
  Learner check: Tap a quick start, confirm the setup disappears without moving the hourglass, watch the sand advance, and use a short valid timer, edited demo footage, or a test-only time mechanism as needed to inspect the unmistakable Finished state, Repeat, and Back. Do not add a permanent acceleration control to the shipping POC.
  Commit: `Add accurate quick-start timer flow`

- [ ] **4. Manual durations and recent history work safely**
  Becomes usable: A user can type or paste 1–1,440 whole minutes, start with the button or IME Done, receive inline validation for bad input, and reuse a locally persisted unique three-item MRU list with correct first-launch versus stored-empty behavior.
  Why now: Once the accurate timer path exists, manual input and persistence can reuse it instead of creating a second start path. The missing-versus-empty rule is isolated and testable before controls add more states.
  PRD ref: `prd.md > Quick Starts and Recent Durations`, `prd.md > Manual Duration and Validation`, `prd.md > Starting a Session`, `prd.md > States and Boundaries`
  Spec ref: `spec.md > Duration Input Validator`, `spec.md > Recent Durations Policy and Repository`, `spec.md > Setup Panel`, `spec.md > Persisted Data`
  Build: Add Preferences DataStore ordered-string persistence, absence-only `[15,25,40]` seeding, defensive decoding, pure MRU policy, process-local manual draft, numeric IME/Done, safe validation, inline error clearing, keyboard dismissal on valid start, and unchanged manual draft when a quick start is used.
  Verify (mechanical): Add and run tests for input boundaries, pasted/overflow/unusual text, MRU reuse/order/trimming, absent key, present empty key, malformed stored value, and process-local draft rules. Run unit tests and `assembleDebug`; use ADB/app-data resets and relaunches to verify one-time defaults and persisted order without reseeding an existing empty value.
  Learner check: On the phone, start a valid manual duration, try invalid and pasted values, return to setup, reuse quick starts, and relaunch. Confirm the field/history behavior matches the approved examples and that no keyboard opens automatically.
  Commit: `Add validated durations and recent history`

- [ ] **5. Tap-revealed controls manage the session without distraction**
  Becomes usable: Running normally shows only the hourglass; a tap reveals stable time and Pause/Resume, Restart, and Back controls; timeout, paused visibility/dismissal, pause indicator, immediate restart/cancel, and system Back rules all behave consistently.
  Why now: The timer and start paths are already authoritative, so interaction controls can be added as transitions around one trusted session rather than debugging time math and gestures together.
  PRD ref: `prd.md > Running and Paused States`, `prd.md > Overlay and Pause`, `prd.md > Restart, Back, and Completion`, `prd.md > Background, Relaunch, and System Back`
  Spec ref: `spec.md > Visual Time Screen`, `spec.md > Timer Overlay and Pause Indicator`, `spec.md > System Back and Input Dispatch`, `spec.md > Finished Panel`
  Build: Add the fixed-width tabular time overlay, labelled touch-friendly controls, five-second monotonic inactivity deadline, tap isolation, pause freeze/resume, paused overlay dismissal and indicator, immediate Restart/Back, keyboard-first and overlay-aware system Back handling, and stable finished controls. Preserve the hourglass bounds through every overlay change.
  Verify (mechanical): Add high-value ViewModel/Compose tests for overlay reveal/hide/timeout, control event consumption, pause/resume precision, paused dismissal, restart, cancellation, system Back phases, fixed-width formats, and finished actions. Run unit tests, connected tests where reliable, and `assembleDebug`; install and exercise the interactions on the phone while checking crash logs.
  Learner check: On the phone, reveal and dismiss controls, pause with controls visible and hidden, resume, restart, and use on-screen/system Back. Confirm controls never move the hourglass or accidentally trigger the background tap.
  Commit: `Add timer overlay and session controls`

- [ ] **6. The complete POC survives its real-device demo journey**
  Becomes usable: The approved core journey works end to end on the Redmi Note 9 Pro, including live-process background catch-up, exact paused return, edge-to-edge safe controls, stable `adjustNothing` keyboard behavior, crash-safe orientation handling, final visual polish, and offline operation.
  Why now: This is integration and device evidence after each risky behavior already works in isolation. It catches Xiaomi-specific IME/inset behavior and lifecycle gaps without adding features or broadening the device matrix.
  PRD ref: `prd.md > The Core Journey`, `prd.md > Background, Relaunch, and System Back`, `prd.md > Look and Feel`, `prd.md > What We're Building`
  Spec ref: `spec.md > Main Activity and Window Policy`, `spec.md > Lifecycle-Aware Update Pulse`, `spec.md > Physical-Device Checklist`, `spec.md > Build and Verification Sequence > 6. Targeted Verification`, `spec.md > Important Failure Modes`
  Build: Complete live-process foreground recomputation, expired-while-backgrounded transition, paused return, safe status/cutout/navigation insets, `adjustNothing`, portrait manifest policy, non-autofocus setup, stable hourglass sizing, theme/text polish, and any small fixes revealed by the agreed phone checks. Do not add process-death session restore, notifications, sounds, landscape polish, settings, Dial, or a custom keypad.
  Verify (mechanical): Run the full deterministic unit suite, targeted Compose/device tests, and `.\gradlew.bat assembleDebug`; reinstall the APK and execute the documented physical-device checklist for IME visibility/Done/Start/Back, overlay isolation, lifecycle return, expiry, MRU relaunch, and obvious performance/runaway work. Inspect final Git status/staged diff and push the verified milestone after a clean commit.
  Learner check: Perform the one-minute demo journey and then deliberately try awkward input, keyboard Back, active-session system Back, background/foreground, pause return, Repeat, and setup return. Note anything broken, confusing, visually weak, or worth refining for the final review.
  Commit: `Complete Visual Time POC verification`

## Hands-on Checkpoints

- [ ] Required pause after slice 2: learner completes the 0%/25%/50%/75%/100% hourglass phone review and visual feedback is resolved before full timer/UI expansion
- [ ] Required pause after slice 6: final kick-the-tires exploration and feedback completed

## Final Review

- [ ] Final review complete — feedback resolved and learner confirms ready to ship

## Code Tour and App Map

- [ ] Learning activity complete — guided route, focused alternative, prior practice connected, or brief recap
- [ ] Optional edit and transfer reflection addressed — offered/declined/already covered/not applicable as appropriate
- [ ] `devpost/app-map.html` generated from finished code, checked, and shown, including a project-grounded practice to reuse

Activity and evidence: [what actually happened; real document/test/code references; unfinished work if interrupted]
Route and stops: [actual paths and symbols; guided stops completed, or reference-only route]
Edit outcome: [tried/kept/reverted/declined/not applicable; verification if changed]
Reflection: [offered/answered/declined/already covered — personal answer belongs only in the ignored profile]
Activity mode: [live app and editor, explicit static fallback, focused alternative, prior practice, or recap]

## Revisions

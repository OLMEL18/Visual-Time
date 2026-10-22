---
doc: prd
status: approved
---

# Visual Time — Product Requirements

A calm, offline Android visual timer for an adult who benefits from perceiving remaining time as a physical quantity rather than an abstract number.

Source: scope.md > The Unique Kernel, scope.md > Who It's For

## The Core Journey

Source: scope.md > The Core Loop, scope.md > What “Working” Looks Like

1. The user opens Visual Time to a portrait-first, black setup surface with a large stationary hourglass already visible.
2. On a fresh installation where no saved quick-start list exists, the top setup panel shows 15, 25, and 40 minutes. Otherwise it shows the saved three-item list, including an intentionally empty stored list if one exists.
3. The user either taps a quick-start value to begin immediately or enters a whole-minute duration from 1 through 1,440 and presses Start or the keyboard Done action.
4. A started duration moves to the front of the unique, locally persisted, three-item most-recently-used list. Merely typing does not change the list.
5. The keyboard is dismissed, both setup panels fade away, and the hourglass begins from a full upper chamber and empty lower chamber without moving or resizing.
6. During normal running, only the hourglass is visible. Sand depletion, flow, and accumulation reflect the actual remaining proportion.
7. A tap reveals remaining time and Pause/Resume, Restart, and Back controls. While running, the overlay hides after five seconds without interaction or immediately after a background tap.
8. Pause freezes the timer and sand. The controls and time remain visible unless the user explicitly dismisses them; a dismissed paused state retains a small pause indicator.
9. Restart immediately begins the same duration from the start. Back immediately cancels the session and restores setup.
10. At zero, the stream stops, all sand rests below, and a persistent Finished state offers Repeat or Back to setup.

## Screens and Layout

Source: scope.md > The POC Boundary

Visual Time uses one full-screen surface whose content changes between setup, running, paused, and finished states. These are not separate navigational screens.

### Setup State

- True-black full-screen background.
- Large stationary Sand hourglass, already in its initial full-upper/empty-lower pose.
- Compact dark quick-start panel anchored at the top:
  - “Visual Time” title.
  - One row containing up to three quick-start duration buttons in current MRU order.
- Separate compact dark manual-entry panel anchored at the bottom:
  - A minutes input labelled for the 1–1,440 range with a Start button beside it.
  - Short inline validation feedback appears near the input when required.
- The input is not focused automatically, and the keyboard does not open on launch or on return to setup.
- When the system numeric keyboard opens, only the bottom manual-entry panel clears it so the input and Start action remain visible. The top quick-start panel and hourglass remain fixed; the hourglass is not resized, recentered, or reflowed.

### Running and Paused States

- The hourglass remains fixed in the same position and size.
- The default running state shows no digits or controls.
- The revealed overlay places remaining time near the top and three clearly labelled, touch-friendly controls along the bottom: Pause/Resume, Restart, and Back.
- Controls sit above the hourglass without changing its layout. Control taps do not also trigger the surrounding show/hide gesture.
- If the paused overlay is dismissed, only the frozen hourglass and a small unobtrusive pause indicator remain.

### Finished State

- The hourglass remains fixed, with an empty upper chamber, no stream, and all sand resting below.
- A quiet but clearly readable “Finished” label and zero in the session's fixed format—00:00 for an original duration under 60 minutes, or 00:00:00 for an original duration of 60 minutes or more—make completion unmistakable without sound.
- Repeat and Back to setup remain visible in a stable bottom control area and never auto-hide.

## Look and Feel

Source: scope.md > Inspiration & Identity

- Calm, premium, minimalist, tactile, and reassuring; never gamified or urgent.
- True-black (#000000) background with restrained warm sand initially around #E2B76B.
- Clean, highly legible Android/system sans-serif typography. Remaining time uses tabular numerals and reserves enough width for 24:00:00 so changing digits do not move the display. Text is light but not glaring white; bold is used sparingly.
- Elegant, balanced hourglass silhouette with subtly translucent cool-gray glass, fine contours and restrained reflections. A dark-metal or warm-neutral frame may support the form without competing with the sand.
- Sand is the focus: natural nonrectangular piles, a fine continuous-looking stream, and believable proportional changes. Avoid dashed lines, repeated-stroke artifacts, and rectangular fills.
- Setup and control surfaces are compact, rounded, dark charcoal/translucent, with subtle outlines, readable labels, and touch targets around 48 dp or larger.
- Start and Repeat may use a restrained sand-colored accent. Prefer short visible labels to cryptic unlabeled icons.
- Avoid bright Material-default colors, heavy gradients, excessive shadows, loud motion, flashing warnings, celebratory effects, and decorative clutter.

## Features and Behavior

### Quick Starts and Recent Durations

Source: scope.md > The POC Boundary, scope.md > Acceptance Criteria

- If and only if the saved-list value is absent, initialize the list once as [15, 25, 40].
- An existing stored list, including an intentionally empty one, is never reseeded.
- Starting any valid duration moves it to the front, removes any duplicate occurrence, trims the list to three entries, and persists the updated order.
- Example: [15, 25, 40] → start 25 → [25, 15, 40] → enter and start 30 → [30, 25, 15].
- Tapping a quick-start value starts it immediately without confirmation.
- The last manually typed value is preserved only while the current app process remains alive.
- Quick-start use does not overwrite a separate manually typed draft during that live process.

### Manual Duration and Validation

Source: scope.md > The POC Boundary, scope.md > Acceptance Criteria

- The field accepts whole-minute values from 1 through 1,440 inclusive.
- It requests the standard Android numeric soft keyboard and a Done/Enter action; the POC has no custom keypad.
- Empty input is neutral while editing.
- A pasted valid whole-number value from 1 through 1,440 is accepted exactly like typed input.
- Pressing Start or keyboard Done with empty, invalid, non-numeric, fractional, zero, negative, oversized, or overflow-prone input does not start a timer and never crashes.
- Invalid submission shows a short inline message near the field, such as “Enter 1–1440 minutes,” without a dialog.
- The message clears once the input becomes valid.
- Valid Start and valid Done behave identically and dismiss the keyboard.
- Android system Back dismisses a visible keyboard without starting or cancelling anything.
- Typing or editing never changes recent-duration history; a manual value joins history only when its timer actually starts.

### Starting a Session

- A valid manual Start, valid Done action, or quick-start tap starts a session and updates the MRU list.
- Any visible keyboard is dismissed.
- Both setup panels fade away smoothly.
- The hourglass stays visually stable and begins at the full-duration pose.
- The clean running state initially shows no numeric countdown or controls.

### Sand Progress and Time Display

Source: scope.md > The Unique Kernel, scope.md > Acceptance Criteria

- Sand progress follows elapsed timer time, not animation-frame counts.
- The upper quantity decreases and lower quantity increases in believable proportion to the selected duration; a fine continuous-looking stream appears only while time is advancing.
- The numeric format is chosen once from the original session duration and never changes as time decreases.
- An original duration under 60 minutes uses MM:SS for the entire session, from values such as 15:00 through 00:01 to 00:00 at completion.
- An original duration of 60 minutes or more uses HH:MM:SS with two-digit hours for the entire session, including values such as 01:30:00, 01:00:00, 00:59:59, 12:00:00, and 24:00:00.
- Minutes and seconds are always zero-padded; hours are also zero-padded in the long format.
- Underlying elapsed-time progress remains accurate and continuous. For presentation, any positive fractional second rounds up, so a running session with time left displays at least one second and never displays zero.
- At actual expiry, the product transitions to Finished, the final sand pose is applied, and zero appears in the fixed session format: 00:00 or 00:00:00.

### Overlay and Pause

- A single tap on the hourglass/background reveals the overlay.
- During running, five seconds without interaction hides it. A background tap hides it immediately. Overlay interaction restarts or changes this period as appropriate.
- Pause freezes countdown progress, the stream, and the sand levels at the same remaining duration.
- While paused, time and controls remain visible indefinitely; the running timeout does not apply.
- A background tap explicitly dismisses the paused overlay, leaving a small pause indicator. Another tap reveals it again.
- Resume continues from the frozen remaining duration. The normal running overlay timeout then applies from that interaction.
- Tapping an overlay control never propagates into the background tap handler.

### Restart, Back, and Completion

- Restart has no confirmation. It restores the full-upper/empty-lower pose, immediately starts the same selected duration, and returns to the clean running view.
- On-screen Back has no confirmation. It cancels the timer, stops all countdown and sand movement, and returns to setup; no hidden session continues.
- Returning to setup within the same live process shows the stationary initial hourglass, current MRU values, and the last manually typed value. Quick-start use does not change that manual value.
- At expiry, the upper chamber is empty, all sand rests below, and the stream stops. “Finished,” zero in the session's fixed format, Repeat, and Back to setup remain visible.
- Repeat has no confirmation. It starts the same duration again, updates the MRU under the same rules, restores the starting sand pose, and returns to the clean running view.
- Finished-state Back returns to setup with current MRU values and the manual entry preserved.
- Completion adds no sound, vibration, notification, or celebratory effect.

### Background, Relaunch, and System Back

Source: scope.md > The POC Boundary, scope.md > Technical Risks

- If the process remains alive while the app is backgrounded, a running session continues to age. On return, the displayed time and sand are recomputed to the correct position; if time expired, the Finished state appears.
- A paused session remains paused at precisely its frozen position when returning to the foreground and retains access to its paused controls.
- Temporarily backgrounding and resuming preserves the manual input while the process remains alive.
- After process death and relaunch, no active session is restored. The app shows normal setup with the independently persisted MRU list and an empty, unfocused manual input; it does not open the keyboard.
- With no keyboard visible, system Back during a running or paused session first reveals the controls if the overlay is hidden.
- With the overlay already visible, system Back cancels the session and returns to setup, matching the on-screen Back action.
- In Finished, system Back returns to setup.

## States and Boundaries

- **Fresh install** — Setup appears with [15, 25, 40], manual input unfocused, and the stationary initial hourglass.
- **Saved empty list** — Setup shows no quick-start buttons and does not restore defaults; manual entry remains usable.
- **Invalid submission** — No timer starts; inline feedback appears and clears after correction.
- **Running, overlay hidden** — Only the moving hourglass is visible.
- **Running, overlay visible** — Time and three controls are visible for up to five seconds without interaction.
- **Paused, overlay visible** — Sand is frozen; time and controls remain indefinitely.
- **Paused, overlay hidden** — Sand is frozen; only a subtle pause indicator distinguishes it from a broken timer.
- **Finished** — Lower chamber is full; Finished, zero in the session's fixed format (00:00 or 00:00:00), Repeat, and Back remain visible.
- **Live-process foreground return** — Running progress catches up, expiration resolves to Finished, and paused progress stays frozen.
- **Process-death relaunch** — No session or manual-input draft returns; setup and persisted quick starts do. The manual field is empty and unfocused.
- **Offline-only boundary** — All POC behavior works without an account, network, backend, ads, or subscription.

## Product Decisions

- Sand is the sole POC visualization so the distinctive visual-time experience receives the available polish.
- A practical timer loop, persisted quick starts, and hidden-by-default controls are protected requirements; the proof is not merely an animation showcase.
- [15, 25, 40] makes a fresh install immediately useful, while absence-based seeding respects any existing stored list.
- Numbers stay hidden during normal running to preserve calm focus, but remain available on demand and become explicit at completion.
- The standard numeric keyboard is sufficient for the POC; a custom keypad does not strengthen the central experiment.
- The manual-input draft is process-local rather than another persisted preference; only quick-start history survives process death.
- Restart and Back act without confirmation to preserve low-friction use; system Back uses a two-step reveal-then-cancel pattern to reduce accidental loss.
- Lightweight visual illusion is preferred to grain-level physics, provided the stream and piles remain convincing.
- Portrait-first presentation protects the short build and demo while preserving the intended visual hierarchy.

## What We're Building

- One portrait-first Android Sand timer surface with setup, running, paused, and finished states.
- Whole-minute input from 1 through 1,440 with safe inline validation.
- Three unique locally persisted quick-start durations with one-time default seeding and MRU behavior.
- Accurate elapsed-time-based countdown and synchronized sand presentation.
- Hidden-by-default running controls, five-second overlay behavior, pause handling, restart, cancellation, completion, and repeat.
- Limited live-process background/foreground correctness and explicit process-death fallback to setup.
- The agreed calm, premium, black-and-warm-sand visual identity.

## Deferred From the POC

- Dial visualization and separate mode histories.
- Background notifications, persistent services, and active-session recovery after process death.
- Sound, vibration, and other completion alerts.
- Orientation-specific polish and a finished landscape layout.
- Always-visible numeric time, Keep Screen On options, selectable colors, and a Settings surface.
- Custom themed numeric keypad.
- Donations, rewarded ads, and other monetization.

## Possible Later Enhancements

- Add the Dial visualization while preserving separate three-duration histories per mode.
- Add opt-in completion signals and robust background timer behavior.
- Add carefully limited appearance and visibility preferences without turning the app into a settings-heavy productivity tool.

## Non-Goals

- No physics-grade individual-grain simulation; visual plausibility matters more than simulation fidelity.
- No accounts, network dependency, database, backend, subscriptions, ads, or analytics.
- No nonfunctional selectors or placeholders for future modes and settings.
- No promise of production-grade battery measurements, process-death recovery, notification delivery, or landscape polish.
- No generic gamification, urgency cues, celebration, or productivity-suite features.

## Open Questions

- None.

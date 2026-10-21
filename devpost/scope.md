---
doc: scope
status: approved
---

# Visual Time

A calm, offline Android visual timer that makes remaining time feel physically visible rather than merely numerical.

## The Unique Kernel

Time becomes a shrinking physical quantity: warm sand leaves the upper chamber, flows continuously through the neck, and accumulates below in proportion to the true time remaining. The proof must feel like a useful, distraction-free timer—not an hourglass animation with timer controls attached.

## Who It's For

An adult, including someone with ADHD, who struggles to sense remaining time and wants to begin a short focused work or study session without entering a feature-heavy productivity system. Today they may use an abstract numeric countdown or lose track of the boundary altogether.

## The Core Loop

The user opens Visual Time, enters a whole-minute duration or taps a familiar quick-start duration, and begins almost immediately. A fresh installation initializes the quick-start list to 15, 25, and 40 minutes, while manual entry and Start are also available. The setup disappears, leaving only the animated hourglass on black. A tap temporarily reveals the remaining time and essential controls; completion provides a clear sense of closure. The user returns because starting a familiar session is quick and the passage of time is understandable at a glance.

## Inspiration & Identity

Quiet, predictable, pleasant, and reassuring rather than urgent. The visual identity is an attractive hourglass silhouette with natural warm sand on a black background, a fine continuous-looking stream, and believable changing sand piles. A lightweight visual illusion is welcome; dashed streams, conspicuous repeated strokes, and rectangular fill effects are not.

## Why This Matters to the Learner

The learner is familiar with how easily time can disappear and wants a simple alternative to productivity apps overloaded with features, accounts, and ads. The project is also a chance to turn an established requirements-led practice into a lightweight, repeatable Codex workflow for small maintainable apps.

## What “Working” Looks Like

A runnable Android proof of concept demonstrates, in about one minute: open the app, tap a recent duration or enter a new one, watch uninterrupted sand reflect elapsed time, tap to reveal the numeric remainder and controls, pause and resume, and reach a clear finished state. A short test duration or edited time jump may compress the demo without compromising timer accuracy. The source is suitable for the required public GitHub repository; deployment is not required.

## The POC Boundary

- One Sand visualization in a single-screen Android experience.
- Whole-minute duration entry from 1–1,440 with validation.
- Three locally persisted, unique quick-start durations in most-recently-used order. Only when the saved-list value is absent, initialize it once to 15, 25, and 40 minutes; an existing stored list, including an intentionally empty one, is never reseeded. Starting any duration moves it to the front without duplication, retains only three entries, and persists the order.
- Tapping a quick-start duration starts immediately; a newly entered duration has a clear Start action.
- Distraction-free countdown by default, with no visible numbers.
- Tap-revealed remaining time and Pause/Resume, Restart, and Back-to-setup controls. While running, the overlay hides by another tap or short inactivity; while paused, it remains visible until Resume or explicit dismissal. Using an overlay control must not also trigger the surrounding tap-to-hide action.
- Back to setup cancels the current session. Restart immediately starts the selected duration again from the beginning. Completion clearly offers repeating that duration or returning to setup.
- Countdown and sand progress derived from elapsed time. If the process remains alive while backgrounded, resuming recomputes remaining time from a monotonic elapsed-time source rather than animation-frame counts.
- A convincing but lightweight hourglass and sand illusion, smooth enough for the demonstration without obvious wasteful animation work.

## Acceptance Criteria

- A valid manually entered duration can start a timer; invalid or out-of-range input cannot.
- When the saved-list value is absent, first launch creates the quick-start order [15, 25, 40]. An existing stored list, including an intentionally empty one, is never repopulated with defaults. Starting a session—not merely typing—moves its duration to the front of the locally persisted, unique, three-item MRU list; tapping a quick-start duration starts immediately. For example: [15, 25, 40] → start 25 → [25, 15, 40] → enter and start 30 → [30, 25, 15].
- During an active session, setup is absent and the default view shows no numbers or controls.
- Sand depletion, flow, and accumulation visibly track elapsed-time progress rather than animation-frame counts.
- A tap reveals remaining time and Pause/Resume, Restart, and Back controls. Running sessions auto-hide the overlay after brief inactivity; paused sessions keep it visible until Resume or explicit dismissal. Control taps do not accidentally invoke the outer hide handler.
- Pause/Resume preserves accurate progress, Restart immediately begins the selected duration again, and Back cancels the session and returns to setup. Expiry clearly offers Repeat and Back to setup.
- After backgrounding and resuming with the process still alive, remaining time is recomputed from a monotonic elapsed-time source.
- The hourglass meets the agreed visual threshold: attractive silhouette, warm natural-looking sand, continuous-looking stream, and believable non-rectangular pile changes.
- The Android proof runs offline and can be demonstrated in about one minute using a short duration or edited cuts.

## Technical Risks

- Visual tuning is the largest uncertainty: making a lightweight illusion look like sand may take several iterations even without grain-level physics.
- Timer, pause, restart, completion, navigation, and temporary-overlay timing must share one consistent state model or visual progress can drift from the countdown.
- Android lifecycle behavior is intentionally limited. A live process must recompute elapsed time correctly after background/resume, and recent durations must survive relaunch. Background notifications, a persistent service, and restoration of an active timer after process death are not promised.
- Smoothness and obvious runaway work can be sanity-checked on a target emulator or device; the POC will not claim measured production-grade battery efficiency.

## Later

- Dial as the second visualization and separate recent histories for Sand and Dial.
- Production-grade background behavior and notifications.
- Optional sound and vibration.
- Orientation-specific polish, Keep Screen On settings, and an always-visible numeric-time option.
- Selectable colors and other deliberate settings.
- Donations or rewarded ads, if ever pursued without weakening the calm core experience.

## Explicitly Cut

- Physics-grade individual sand-grain simulation: it threatens the deadline without being necessary for a convincing visual result.
- Dial in the POC: polishing one distinctive visualization is the coherent experiment.
- A Settings screen and nonfunctional selectors: they do not prove the core experience.
- Accounts, backend, database, subscriptions, and ads: local preferences are sufficient, and this proof is intentionally private-by-default and distraction-free.
- Sound, vibration, notifications, production-grade background execution, orientation polish, and monetization: each adds platform or testing surface without strengthening the central visual-time demonstration.

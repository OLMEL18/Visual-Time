# Visual Time

Visual Time is a calm, distraction-free Android timer that makes remaining
time visible instead of relying exclusively on a numeric countdown. It may be
useful for focused work, study sessions, breaks, and people who experience
time blindness or ADHD-related difficulty perceiving how much time remains.

The hackathon proof of concept uses native Kotlin, Jetpack Compose, and a
custom Canvas-drawn hourglass. The core timer works offline and has no account,
ads, analytics, subscription, backend, or runtime network requirement.

## Implemented Features

- Animated Sand hourglass with monotonic elapsed-time accuracy.
- Quick-start buttons backed by three unique, locally persisted recent
  durations, initially 15, 25, and 40 minutes.
- Validated manual entry from 1 to 1,440 whole minutes.
- Distraction-free running view with no numeric countdown by default.
- Tap-revealed remaining time and Pause/Resume, Restart, and Back controls.
- Clear Finished state with Repeat and Back to setup actions.
- Screen-awake behavior while a timer needs attention.
- Gentle foreground completion feedback and a background completion
  notification. Delivery and precision depend on Android permissions and
  device restrictions; exact delivery is not guaranteed under every OS
  configuration, and a running session is not restored after device reboot.
- Setup-only About dialog with project, version, developer, and legal links.

## Platform and Development Requirements

- Android 8.0 or later (`minSdk 26`).
- Android SDK Platform 37 (`compileSdk 37`, `targetSdk 36`).
- JDK 17.
- Android Studio with the Android SDK, or Gradle 9.4.1 through the included
  wrapper.

Android Studio normally creates the ignored `local.properties` file containing
the local Android SDK path when the project is opened.

## Build and Install

### Android Studio

1. Clone the repository and open its root folder in Android Studio.
2. Allow Gradle sync to finish and ensure SDK Platform 37 is installed.
3. Select the `app` configuration and an Android 8.0+ device.
4. Choose **Run** to build and install the debug application.

### Gradle and ADB

From PowerShell in the repository root, run the unit tests and build the debug
APK:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

On macOS or Linux, use:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

With an authorized Android device connected, install it with:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Test the Main Journey

1. Open Visual Time and tap a quick-start duration; the timer starts
   immediately.
2. Tap the hourglass to reveal the remaining time and controls.
3. Pause and resume, then try Restart and Back to setup.
4. Enter a valid manual duration and confirm it moves to the front of the
   recent-duration list only when its timer starts.
5. Use a one-minute session to verify the Finished state, Repeat, and Back to
   setup.
6. Optionally background or lock the device during a short session to verify
   the completion notification. Android may request notification or exact-alarm
   access depending on the OS version.

## Planning and Code Map

Visual Time was built with the Devpost Learn Skill Pack. The repository keeps
the public planning and verification artifacts:

- [Proof-of-concept scope](devpost/scope.md)
- [Product requirements](devpost/prd.md)
- [Technical specification](devpost/spec.md)
- [Build checklist and verification record](devpost/checklist.md)
- [Offline code tour and app map](devpost/app-map.html)

The personal learner profile is intentionally excluded from version control.

## License and Branding

The source code and associated documentation are available under the
[MIT License](LICENSE).

The Visual Time and KISS Studio names, logos, original launcher icon, and
derived launcher artwork are not licensed under MIT. They are included so the
application can be built, run, demonstrated, and evaluated. See
[`BRANDING.md`](BRANDING.md) for the exact asset scope and permitted use.

Third-party libraries and tools retain their own licenses.

## KISS Studio

Developed by **Keep It Simple Software Studio (KISS Studio)**.

- [Website](https://kiss.slet.org.ua/)
- [Privacy Policy](https://kiss.slet.org.ua/privacy.html)
- [Terms of Use](https://kiss.slet.org.ua/terms.html)

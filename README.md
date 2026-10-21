# Visual Time

Visual Time is a calm, offline Android visual timer that represents remaining time as warm sand moving through an hourglass.

The hackathon proof of concept uses native Kotlin, Jetpack Compose, and a custom Canvas. It targets Android 16 behavior (`targetSdk 36`), compiles with SDK 37, and supports Android 8.0 and later (`minSdk 26`).

## Local build

Requirements:

- JDK 17
- Android SDK Platform 37
- An Android SDK path in the ignored `local.properties`

Build the debug APK from PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

## Privacy

The application has no account, backend, analytics, ads, or runtime network requirement. The local Devpost learner profile and machine-specific Android configuration are excluded from Git.

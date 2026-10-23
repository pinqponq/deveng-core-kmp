# Device setup for the pinqloq task runner

The sample app (`sample/composeApp`) is the device target for deveng-core-kmp. It needs no sign-in and no test account.

- Android app id: `io.deveng.core.androidApp`. Build: `./gradlew :sample:composeApp:assembleDebug`; the APK lands in `sample/composeApp/build/outputs/apk/debug/`.
- Emulator: AVD `Medium_Phone`, cold boot is fine. Install with `adb install -r <apk>`.
- Run a flow: `maestro --device <id> test maestro/motion/reveal.yaml --test-output-dir build/maestro/android`.
- The reveal flow clears the app state, opens the motion demo from the main list and drives pop, flip and count-up.

# Device setup for the pinqloq task runner

The sample app (`sample/composeApp`) is the device target for deveng-core-kmp. It needs no sign-in and no test account.

- Android app id: `io.deveng.core.androidApp`. Build: `./gradlew :sample:composeApp:assembleDebug`; the APK lands in `sample/composeApp/build/outputs/apk/debug/`.
- Emulator: AVD `Medium_Phone`, cold boot is fine. Install with `adb install -r <apk>`.
- Run a flow: `maestro --device <id> test maestro/motion/reveal.yaml --test-output-dir build/maestro/android`.
- The reveal flow clears the app state, opens the motion demo from the main list and drives pop, flip and count-up.
- iOS: the simulator app is built with `xcodebuild -project sample/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator`; its bundle id is `io.pinqponq.chat.iosApp`. Pass it to a flow that reads `APP_ID` with `-e APP_ID=io.pinqponq.chat.iosApp -e PLATFORM=ios`.
- `platformconfig/operating-system-version.yaml` needs `-e EXPECTED_OPERATING_SYSTEM_VERSION=<version>`: on Android the output of `adb shell getprop ro.build.version.release`, on iOS the simulator runtime version (for example `26.0`).
- `session/http-auth-session-api.yaml` opens the Auth Session demo, signs in, renews, signs out during a renewal and signs out again; the demo sends every request through `HttpAuthSessionApi` to a mock backend and lists it under "Events".

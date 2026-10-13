# WearBreathe

Wear OS take on Apple's Breathe: a phyllotaxis "flower" that blooms on inhale and collapses to a glowing orb on exhale,
with haptics that rise on inhale and fade on exhale so you can follow it eyes-closed.

Open this folder in Android Studio (Koala or newer), let Gradle sync, run on a Wear OS 3+ device or emulator.

Tuning knobs:
- Haptics.kt  -> ramp(): gap / pulse length / amplitude ranges
- BloomCanvas.kt -> DOTS, radius and spread constants, colours
- BreatheApp.kt -> INHALE_SHARE (inhale vs exhale split), default minutes and breaths/min

## Galaxy Watch 7
Targets Wear OS 5 (API 34), round 432px (40mm) / 480px (44mm) AMOLED. The bloom scales from the screen size.
Haptics strength (Soft / Med / Firm) is set on the start screen and remembered.

## CI
.github/workflows/build.yml builds a debug APK on every push and uploads it as the `WearBreathe-debug` artifact.
Sideload: enable Developer options on the watch, turn on Wi-Fi debugging, then `adb connect <ip:port>` and `adb install -r app-debug.apk`.

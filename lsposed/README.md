# Gboard Fusion LSPosed (Preview 0.1)

This is an **independent runtime module APK** for the original Google-signed Gboard.
No APK patching, no modified Gboard signature, and no package replacement.

## Implemented, carefully scoped

- ✅ Candidate-strip emoji opacity: `AutoCenterScaleTextView.onDraw(Canvas)` (enabled by default)
- ✅ Features menu accessibility: `AccessPointsPanel*` UI lifecycle (enabled by default)
- 🧪 Foreground clipboard self-listener refresh: `fml.ft(...EditorInfo...)` (OFF by default; excludes password and no-personalized-learning fields)
- 🧪 Material-style rounded key boolean flags: `nxw.g()` (OFF by default)
- 🧪 Deduplicate 18.3/18.4 voice language pack queue: `sqk.c` / `snm.c` (OFF by default; short TTL)

Every optional hook checks the full target class, method signature and return type.
Missing or renamed targets do not prevent normal Gboard startup.
No privileged system/framework scope and no INTERNET permission.

## What is NOT integrated

The two original Morphe/Mpp projects expose many resource and bytecode patches (44 published in Gboard-patches). **Those are not automatically reproduced** in LSPosed. Most theme replacements, added activities, resource edits, APK package rename, FTP/web clipboard, embedded extension calls, and deep DEX editing need separate rewrites and a versioned target database. This preview does not claim parity with either patched APK.

## Install

1. Keep the official Gboard installed. Do **not** install GboardFusion-InPlace or a renamed modded keyboard.
2. Install the `GboardFusion-LSPosed-0.1.0.apk` module APK.
3. Enable it in LSPosed (legacy Xposed API) with only the **Gboard** scope `com.google.android.inputmethod.latin`.
4. Open module UI to control safe vs experimental features; force-stop Gboard after settings changes.
5. Review LSPosed logs for `GboardFusionLSP`, which reports number of hooks found (zero = unmatched target).

### Constraints

Legacy Xposed entry `assets/xposed_init` and LSPosed metadata are used to support commonly deployed legacy-compatible frames; this is not a modern API 101+ module. Per-device LSPosed compatibility and Android 17 runtime must be tested. Module settings are exported only as five non-sensitive read-only boolean values through its own local ContentProvider (no write access and no network).

## Build

Java 17 + Android SDK 36 + Android Gradle Plugin 9.0.1:
`./gradlew -p lsposed :app:assembleRelease` from repository root.
API stubs build as a separate `compileOnly` project, are not bundled into APK.
CI workflow is `.github/workflows/gboard-lsposed.yml`.
Attributions: `Akshayykadam/PixelBoard` and `jasonwu1994/Gboard-patches`, both GPL-3.0.

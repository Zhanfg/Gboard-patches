# PixelBoard → Gboard-patches fusion (experimental)

Source integration branch: `feature/pixelboard-fusion`. This is a source-level port, **not** a merge of two signed APKs or two prebuilt `.mpp` files.

## Baselines and attribution

- **Primary:** [jasonwu1994/Gboard-patches](https://github.com/jasonwu1994/Gboard-patches), GPL-3.0, patched fork `Zhanfg/Gboard-patches`.
- **Donor:** [Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard), GPL-3.0, patched fork `Zhanfg/PixelBoard`.
- Both projects use the Morphe 1.3.3 patch plugin. This branch keeps the **44 existing public patches** and does not modify either upstream or either fork's `main`.
- Licensing: retain existing GPL-3.0 and original attribution; transferred implementation is based on PixelBoard.

## Integrated differences

| PixelBoard capability | Patch integration | Runtime |
| --- | --- | --- |
| Repeat speech language-pack download guard | `advancedvoice/GboardAdvancedVoice1831LanguageDownloadGuardPatch.kt` | `GboardLanguageDownloadGuard` |
| Candidate emoji paint opacity | `emoji/GboardCandidateEmojiVibrancyPatch.kt` | `GboardCandidateEmojiRuntime` |
| Foreground clipboard synchronization | `clipboard/GboardClipboardSyncPatch.kt` | `GboardClipboardSyncRuntime` |
| TalkBack features-menu focus | `accessibility/GboardFeaturesMenuAccessibilityPatch.kt` | `GboardFeaturesMenuAccessibilityRuntime` |
| Android 12+ spell-check suggestion chips | `spellchecker/GboardSpellCheckerChipsPatch.kt` | bytecode only |

The new features share **one** Gboard-patches `extensions/gboard-patches.rve` carrier, one `GboardPatchRegistry`, and one settings entry. They do not install the PixelBoard extension as a second runtime or duplicate the four published patches (AI Writing Tools, Advanced Voice Typing, Signature Bypass, Package Rename). PixelBoard's additional defaults and device-dependent AI UI are not yet fully transplanted; see limitations.

## Compatibility boundaries

- The primary Gboard-patches admission profile is currently **18.0.3.954559732-release-arm64-v8a**. Its version bindings must remain unchanged until independently verified.
- Donor PixelBoard declares **18.0.3 / 18.3.1 / 18.4.1**, but its additional bytecode targets include 18.3.1- and 18.4.1-specific obfuscated classes.
- The donor-specific bytecode patches use optional target matching: **no crash is not proof a feature was applied**. Do not market this branch as complete 18.4.1 support. Confirm target matches with a patch report and device tests before adding another compatible package entry.
- Gboard-patches' existing UI/resource work remains the source of truth. This branch deliberately avoids PixelBoard's unilateral clipboard, screenshot, emoji, and AI-inference default overrides to reduce unpredictable behavior.
- Gboard 18.4.1 migration will require rebinding and testing **all 44** original public patches, not just these donor features.
- No PixelBoard stock APK, signed APK, keystore, or binary `.mpp` is merged.

## Build and QA

Build in CI with Java 21, Android SDK and Morphe registry read access (`MORPHE_PACKAGES_TOKEN` if the automatically provided `GITHUB_TOKEN` cannot access the registry):

```bash
./gradlew test :patches:buildAndroid
```

Expected output: `patches/build/libs/patches-*.mpp`.

Before publishing an APK, test supported source Gboard APK on-device: cold boot/direct boot; normal and password fields; English/Chinese keyboards; dictation and language downloads; candidate emoji; clipboard permissions and history; TalkBack focus; AI writing; and all pre-existing Gboard-patches modules. Separate Android 17 and non-Pixel testing is required.

### Known remaining merge work

1. Reconcile PixelBoard writing-tools and Rambler fixes with the corresponding existing Gboard-patches modules (do not register twice).
2. Port PixelBoard 18.4.1 preferences, Material 3 Belka/emoji options and any unique flag changes **only after** verifying base-vs-donor behavior.
3. Versioned class/method/field fingerprints, reproducible Gboard APK tests, release signing and a real device test matrix.
4. Audit unbounded guard cache and clipboard data boundaries before enabling broad production rollout.

**Status:** implemented in source, build/functional test status must be read from GitHub Actions. A passing compilation alone is not an Android device compatibility guarantee.

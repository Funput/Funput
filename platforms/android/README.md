# Funput for Android

Native Android shell and keyboard UI for Funput. The Android app is written in
Kotlin; Vietnamese composition comes from the shared Rust engine through JNI.

## Modules

| Module | Responsibility |
|---|---|
| `app` | Compose host for onboarding, settings, and keyboard previews |
| `ime` | Android input-method lifecycle and focused-editor bridge |
| `keyboard-ui` | Panel navigation and the lazy-loaded AndroidX emoji picker |
| `keyboard-renderer` | Responsive keyboard layout, touch, accessibility, and Canvas rendering |
| `theme-runtime` | Versioned theme contract, validation, token resolution, and safe asset access |

Dependency direction is intentionally one-way:

```text
app -> ime -> keyboard-ui -> keyboard-renderer -> theme-runtime
app --------> keyboard-ui ----------------------> theme-runtime
```

The app persists the Vietnamese input method with Preferences DataStore. VNI is
the first-run default; selecting Telex, Advanced Telex, or VNI in the app updates
the active IME through the same observable setting.

The IME owns Android composing spans while `funput-engine` remains the only
source of truth for Telex, Advanced Telex, and VNI rules.

## Build

Open this directory in the latest stable Android Studio, or run:

```bash
./gradlew :app:assembleDebug
```

The Gradle build cross-compiles `funput-jni` for `arm64-v8a` and `x86_64`.
Install the matching Rust standard libraries once:

```bash
rustup target add aarch64-linux-android x86_64-linux-android
```

The project requires Android SDK 37, NDK 29, and supports Android 8.0 (API 26)
or newer. The NDK version is pinned for reproducible native builds.

## Release

`Deploy Android` (`.github/workflows/deploy-android.yml`) is manual, like its iOS
counterpart: run it from Actions on whichever branch you want to ship, type the
marketing version, and pick a track. It builds the signed `.aab`, uploads it to
Google Play, and attaches the ProGuard mapping so crash reports stay readable.

The **version code is not committed** — it is derived from the workflow's run
number, so shipping needs no version bump in the tree and two deploys can never
claim the same code. `versionCode`/`versionName` in `app/build.gradle.kts` are only
what a local build gets; CI passes `-Pfunput.versionCode` and `-Pfunput.versionName`
over the top.

`dry_run` builds and signs without uploading. The `.aab`, its SHA-256 and the
mapping are attached to every run either way.

Secrets the workflow needs are listed in its header. Uploading is not releasing:
the build reaches the track you picked, and promoting it to production stays a
deliberate act in the Play Console.

See [`docs/ARCHITECTURE_PROPOSAL.md`](docs/ARCHITECTURE_PROPOSAL.md) for the
long-term architecture. The repository is licensed under the
[`MIT License`](../../LICENSE).

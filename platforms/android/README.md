# Funput for Android

Native Android shell and keyboard UI for Funput. The Android app is written in
Kotlin; Vietnamese composition will be provided by the shared Rust engine.

## Modules

| Module | Responsibility |
|---|---|
| `app` | Compose host for onboarding, settings, and keyboard previews |
| `keyboard-ui` | Panel navigation and the lazy-loaded AndroidX emoji picker |
| `keyboard-renderer` | Responsive keyboard layout, touch, accessibility, and Canvas rendering |
| `theme-runtime` | Versioned theme contract, validation, token resolution, and safe asset access |

Dependency direction is intentionally one-way:

```text
app -> keyboard-ui -> keyboard-renderer -> theme-runtime
                   \--------------------> theme-runtime
```

IME integration, Rust JNI, billing, and the remote theme store will be added only
after the renderer prototype is stable.

## Build

Open this directory in the latest stable Android Studio, or run:

```bash
./gradlew :app:assembleDebug
```

The project currently requires Android SDK 37 and supports Android 8.0 (API 26)
or newer.

See [`docs/ARCHITECTURE_PROPOSAL.md`](docs/ARCHITECTURE_PROPOSAL.md) for the
long-term architecture. The repository is licensed under the
[`MIT License`](../../LICENSE).

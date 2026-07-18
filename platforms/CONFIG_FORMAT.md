# Funput configuration interchange format

The versioned JSON document Funput reads/writes for **Export/Import** (and, later,
sync). It is the shared contract every platform implements so a file exported on
one machine imports on another. macOS is the first implementation
(`platforms/macos/Funput/Model/ConfigDocument.swift`); Windows and Linux follow.

## Design rules

- **Portable vs platform-specific.** `preferences` and `shortcuts` use portable wire
  values, although a platform may not expose every input method. Anything tied to
  one OS lives under `platform.<name>` and applies **only** on that platform.
- **Stable enums.** Enum values are lowercase strings (`"telex_advanced"`,
  `"modern"`), not internal numeric rawValues.
- **Non-destructive import (merge).** Import never deletes user data:
  - `shortcuts` merge by `trigger`; an incoming entry overwrites the expansion of a
    matching trigger and new triggers are appended. Nothing is removed.
  - Supported `preferences` overwrite the matching setting. Missing or unsupported
    enum values leave the existing local preference untouched.
  - `platform.<current>` hotkeys apply only when present; `excludedApps` are unioned
    by id.
- **Forward compatible.** Unknown keys are ignored. Missing optional fields are
  skipped. A file whose top-level `schema` differs from `app.funput.config` is
  rejected. A `version` newer than the reader's is accepted on a best-effort basis
  (unrecognised fields ignored) with a note to the user.

## Schema (version 1)

```json
{
  "schema": "app.funput.config",
  "version": 1,
  "exportedAt": "2026-07-03T10:00:00Z",
  "source": { "platform": "macos", "appVersion": "1.2026.42" },

  "preferences": {
    "inputMethod": "telex",          // "telex" | "telex_advanced" | "vni"
    "toneStyle": "traditional",      // "traditional" | "modern"
    "smartEnglishRestore": true,
    "eagerRestore": true,
    "spellCheck": false,
    "autoCapitalize": false
  },

  "shortcuts": [
    { "trigger": "vn", "expansion": "Việt Nam" }
  ],

  "platform": {
    "macos": {
      "toggleShortcut": { "keyCode": 42, "modifiers": 262144, "label": "\\" },
      "flipShortcut": null,
      "excludedApps": [ { "id": "com.apple.Safari", "name": "Safari" } ]
    }
  }
}
```

### Fields

| Field | Portable | Notes |
|---|---|---|
| `schema` | — | Must equal `app.funput.config`. Required. |
| `version` | — | Format version (currently `1`). Required. |
| `exportedAt` | — | ISO-8601 timestamp. Metadata only. |
| `source` | — | `platform` + `appVersion` that produced the file. Metadata only. |
| `preferences.inputMethod` | ✅ | `telex`, `telex_advanced`, or `vni`. Advanced Telex is currently desktop-only and exposed on macOS; unsupported consumers keep their current selection. |
| `preferences.*` | ✅ | Other typing options; each is optional and applied only if present. |
| `shortcuts[]` | ✅ | `{ trigger, expansion }`. Local UUIDs are dropped; recreated on import. |
| `platform.macos.toggleShortcut` | ❌ | `KeyCombo` (`keyCode` is AppKit-specific). Applied only on macOS, only if present. |
| `platform.macos.flipShortcut` | ❌ | `KeyCombo` or `null`. Applied only on macOS, only if present. |
| `platform.macos.excludedApps[]` | ❌ | `{ id (bundleId), name }`. macOS-only; unioned by `id`. |

### `platform.windows`

Windows supports both hotkey presets and user-recorded virtual-key combinations,
and identifies apps by exe name, so its block differs from macOS:

```json
"platform": {
  "windows": {
    "toggleHotkey": "ctrl_backtick",
    "toggleCombo": {
      "vk": 86,
      "ctrl": true,
      "alt": false,
      "shift": true,
      "win": false,
      "label": "V"
    },
    "flipHotkey": "off",
    "flipCombo": null,
    "excludedApps": [ { "id": "code.exe", "name": "VS Code" } ]
  }
}
```

| Field | Notes |
|---|---|
| `toggleHotkey` | Preset id: `ctrl_backtick` \| `ctrl_space` \| `alt_shift`. |
| `toggleCombo` | Optional recorded combo `{ vk, ctrl, alt, shift, win, label }`; when present, overrides `toggleHotkey`. |
| `flipHotkey` | Preset id: `off` \| `ctrl_shift_z` \| `ctrl_shift_x`. |
| `flipCombo` | Optional recorded combo; when present, overrides `flipHotkey`. |
| `excludedApps[]` | `{ id (lowercased exe name), name }`. Unioned by `id`. |

`vk` is the Win32 virtual-key captured using the user's active keyboard layout;
`label` is persisted for display. These fields are Windows-only and are applied
only when running on Windows. Older readers ignore the unknown combo fields, and
Linux continues to read only `platform.linux`.

### `platform.linux`

Same shape as Windows (shared hotkey presets), but apps are identified by their
Fcitx5 `program()`/WM_CLASS, not exe name:

```json
"platform": {
  "linux": {
    "toggleHotkey": "ctrl_backtick",
    "flipHotkey": "off",
    "excludedApps": [ { "id": "code", "name": "VS Code" } ]
  }
}
```

| Field | Notes |
|---|---|
| `toggleHotkey` | Preset id: `ctrl_backtick` \| `ctrl_space` \| `alt_shift`. |
| `flipHotkey` | Preset id: `off` \| `ctrl_shift_z` \| `ctrl_shift_x`. |
| `excludedApps[]` | `{ id (WM_CLASS / program name), name }`. Unioned by `id`. |

Linux-only, applied only when running on Linux. Note: Windows and Linux hotkey
presets are identical, but each still reads only its own block (exe names ≠
WM_CLASS), so hotkeys/apps do not cross between them.

### Not exported

Runtime/local-only state is never written. macOS: `vietnameseEnabled`,
`hasCompletedOnboarding`, `showMenuBarIcon`, `launchAtLogin`. Windows/Linux:
`enabled`, `hasCompletedOnboarding`, `launchAtLogin`.

## Adding a platform

Add a `platform.<name>` block (e.g. `platform.windows`, `platform.linux`) with that
OS's non-portable data, and map it to/from the native config store. Do not put
portable settings under `platform`.

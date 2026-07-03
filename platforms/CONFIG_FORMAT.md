# Funput configuration interchange format

The versioned JSON document Funput reads/writes for **Export/Import** (and, later,
sync). It is the shared contract every platform implements so a file exported on
one machine imports on another. macOS is the first implementation
(`platforms/macos/Funput/Model/ConfigDocument.swift`); Windows and Linux follow.

## Design rules

- **Portable vs platform-specific.** `preferences` and `shortcuts` apply on every
  platform. Anything tied to one OS (hotkey key codes, app identifiers) lives under
  `platform.<name>` and is applied **only** when running that platform.
- **Stable enums.** Enum values are lowercase strings (`"telex"`, `"modern"`), not
  internal numeric rawValues.
- **Non-destructive import (merge).** Import never deletes user data:
  - `shortcuts` merge by `trigger`; an incoming entry overwrites the expansion of a
    matching trigger and new triggers are appended. Nothing is removed.
  - `preferences` overwrite the matching setting; fields absent from the file are
    left untouched.
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
    "inputMethod": "telex",          // "telex" | "vni"
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
| `preferences.*` | ✅ | Typing options; each is optional and applied only if present. |
| `shortcuts[]` | ✅ | `{ trigger, expansion }`. Local UUIDs are dropped; recreated on import. |
| `platform.macos.toggleShortcut` | ❌ | `KeyCombo` (`keyCode` is AppKit-specific). Applied only on macOS, only if present. |
| `platform.macos.flipShortcut` | ❌ | `KeyCombo` or `null`. Applied only on macOS, only if present. |
| `platform.macos.excludedApps[]` | ❌ | `{ id (bundleId), name }`. macOS-only; unioned by `id`. |

### Not exported

Runtime/local-only state is never written: `vietnameseEnabled`,
`hasCompletedOnboarding`, `showMenuBarIcon`, `launchAtLogin`.

## Adding a platform

Add a `platform.<name>` block (e.g. `platform.windows`, `platform.linux`) with that
OS's non-portable data, and map it to/from the native config store. Do not put
portable settings under `platform`.

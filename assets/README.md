# Shared assets

Logo and brand files for all Funput platforms. **Single source of truth** — copy or reference from here; do not fork logos per platform.

## Layout

```
assets/
├── logo.png                 # 512×512 PNG, transparent — README, web UI, marketing
├── svg/funput_logo.svg      # master vector
└── png/
    ├── transparent/         # app icons, UI (logo@16 … logo@1024)
    └── background/          # variants with background fill
```

## Sync to web UI

About screen reads `platforms/ui/public/logo.png`. After changing the logo:

```sh
cp assets/logo.png platforms/ui/public/logo.png
```

## macOS app icon

`platforms/macos/Funput/Assets.xcassets/AppIcon.appiconset/` holds Xcode icon sizes. When the logo changes, refresh those PNGs from `assets/png/transparent/` (or re-export from the SVG).

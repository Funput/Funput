# Emoji catalog attribution

`EmojiCatalog.json` is generated from Unicode Emoji 15.1 `emoji-test.txt`.
Names and category assignments are derived from the Unicode Character Database.
English and Vietnamese search names and keywords come from Unicode CLDR 48.2
character annotations.
Skin-tone modifier sequences are intentionally omitted from Funput's first
emoji picker release.

Copyright © 1991–2023 Unicode, Inc. All rights reserved. Distributed under the
[Unicode License v3](https://www.unicode.org/license.txt).

Source: https://www.unicode.org/Public/emoji/15.1/emoji-test.txt
CLDR source: https://github.com/unicode-org/cldr/tree/release-48

Regenerate by passing `emoji-test.txt`, the output path, `--cldr-root` pointing
to a CLDR 48.2 checkout, and `--cldr-version 48.2`.

The iOS resource is the canonical generated output. Android's
`keyboard-ui:syncEmojiCatalog` task copies those exact bytes into its generated
assets, and `keyboard-ui:verifyEmojiCatalogParity` runs before every Android
build. Regeneration therefore uses one command:

```sh
platforms/ios/Scripts/generate-emoji-catalog.py emoji-test.txt \
  platforms/ios/Packages/FunputKit/Sources/KeyboardRenderer/Resources/EmojiCatalog.json \
  --cldr-root cldr --cldr-version 48.2
```

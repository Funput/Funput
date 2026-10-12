/// The letters page of the stock iPad keyboard.
///
/// ```
/// tab        q w e r t y u i o p   delete
/// caps lock  a s d f g h j k l     return
/// shift      z x c v b n m , .     shift
/// globe  .?123     space     .?123  hide
/// ```
///
/// Email swaps the comma for `@` and URL for `/`; those fields and passwords drop the
/// Telex hints, the Vietnamese alternates and the language swipe on the space bar.
enum PadLetterKeyboardLayouts {
    private typealias Keys = PadKeyFactory

    static func letters(_ page: PadPage) -> KeyboardLayout {
        let method = page.inputMethod
        let digits = page.composes
            ? topNumberRow(for: method, pageID: "pad-\(method.rawValue)")
            : topNumberRow(.plainCharacter, pageID: "pad\(page.editorID)")
        let layout = KeyboardLayout(
            id: "qwerty-pad\(page.editorID)-\(method.rawValue)"
                + (page.isCompact ? "-compact" : ""),
            inputMethod: method,
            toolbar: page.toolbar,
            rows: (page.isCompact ? [] : [Keys.numberRow(digits)]) + [
                row(
                    leading: Keys.leadingKey("tab", "tab", .tab, "Tab"),
                    letters: "qwertyuiop",
                    trailing: Keys.deleteKey(),
                    page: page
                ),
                row(
                    leading: Keys.leadingKey("capslock", "caps lock", .capsLock, "Caps Lock", weight: Keys.wide),
                    letters: "asdfghjkl",
                    trailing: Keys.returnKey(),
                    page: page
                ),
                bottomRow(page),
                Keys.actionRow(
                    switchLabel: ".?123",
                    switchRole: .symbols,
                    switchAccessibility: "Ký hiệu",
                    composes: page.composes
                ),
            ]
        )
        return page.isCompact ? CompactDigitAlternates.decorate(layout) : layout
    }

    private static func letterKeys(_ characters: String, _ page: PadPage) -> [KeySpec] {
        characters.map {
            characterKey(
                $0,
                showsTelexHint: page.composes && page.inputMethod.isTelexFamily,
                supportsVietnameseAlternates: page.composes
            )
        }
    }

    private static func row(
        leading: KeySpec,
        letters: String,
        trailing: KeySpec,
        page: PadPage
    ) -> KeyboardRow {
        KeyboardRow(keys: [leading] + letterKeys(letters, page) + [trailing])
    }

    /// `shift z … m , . shift`; the stock keys print `!` and `?` above the comma and period.
    private static func bottomRow(_ page: PadPage) -> KeyboardRow {
        KeyboardRow(keys:
            [Keys.leadingKey("shift", "shift", .shift, "Shift", weight: Keys.wide)]
                + letterKeys("zxcvbnm", page) + [firstPunctuation(page.editorMode), period]
                + [Keys.trailingKey("shift-right", "shift", .shift, "Shift", weight: Keys.wide)]
        )
    }

    /// What sits beside `m`: the comma, or the character the field reaches for.
    private static func firstPunctuation(_ editorMode: KeyboardEditorMode) -> KeySpec {
        switch editorMode {
        case .email:
            KeySpec(id: "at-pad", label: "@", role: .punctuation, accessibilityLabel: "A còng")
        case .url:
            KeySpec(id: "slash-pad", label: "/", role: .punctuation, accessibilityLabel: "Dấu gạch chéo")
        default:
            KeySpec(
                id: "comma-pad",
                label: ",",
                role: .punctuation,
                secondaryLabel: "!",
                accessibilityLabel: "Dấu phẩy"
            )
        }
    }

    private static let period = KeySpec(
        id: "period-pad",
        label: ".",
        role: .punctuation,
        secondaryLabel: "?",
        accessibilityLabel: "Dấu chấm",
        alternates: PunctuationKeyAlternates.period,
        alternateColumns: PunctuationKeyAlternates.periodColumns
    )
}

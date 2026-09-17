import Foundation

/// Where Apple puts the keys of its letters and action rows on Face ID iPhones, on the
/// ten-column letter grid. Measured on iOS 27 at 390–440pt; see
/// `docs/KEY_ACCURACY_INVESTIGATION.md`.
enum SystemRowSpans {
    /// `<switch> <space> <enter>`. The switch key ends one gap before `x`, the spacebar
    /// runs from under `x` to the end of `n`, and enter takes the rest. Apple has no emoji
    /// key here — on these phones it sits below the keyboard, and in Funput's toolbar.
    static let action: [KeyColumnSpan] = [
        KeyColumnSpan(start: .leading, end: .grid(columns: 2.5, gaps: -1)),
        KeyColumnSpan(start: .grid(columns: 2.5), end: .grid(columns: 7.5, gaps: -1)),
        KeyColumnSpan(start: .grid(columns: 7.5), end: .trailing),
    ]

    /// The action row with an emoji key after the switch key, for a layout without a
    /// toolbar to carry one. The switch key gives up half its width so the spacebar and
    /// enter stay exactly where muscle memory expects them.
    static let actionWithEmoji: [KeyColumnSpan] = [
        KeyColumnSpan(start: .leading, end: .grid(columns: 1.25, gaps: -1)),
        KeyColumnSpan(start: .grid(columns: 1.25), end: .grid(columns: 2.5, gaps: -1)),
    ] + action.dropFirst()

    /// `shift z x c v b n m delete`, with the letters under `s`…`l`.
    ///
    /// Without a number row Apple leaves about a quarter key plus two-thirds of a gap
    /// between Shift and `z` (13.6pt at 390pt, 14.6pt at 440pt); with one, Shift and Delete
    /// grow to a plain gap from the letters.
    static func bottomLetters(hasNumberRow: Bool) -> [KeyColumnSpan] {
        let shiftEnd: KeyColumnSpan.Anchor = hasNumberRow
            ? .grid(columns: 1.5, gaps: -1)
            : .grid(columns: 1.25, gaps: -2.0 / 3.0)
        let deleteStart: KeyColumnSpan.Anchor = hasNumberRow
            ? .grid(columns: 8.5)
            : .grid(columns: 8.75, gaps: -1.0 / 3.0)
        return [KeyColumnSpan(start: .leading, end: shiftEnd)]
            + (0..<7).map { KeyColumnSpan.letter(at: 1.5 + CGFloat($0)) }
            + [KeyColumnSpan(start: deleteStart, end: .trailing)]
    }
}

/// Width weights for action-row keys that are not placed on the grid.
enum SystemActionRowWeights {
    /// Narrower than a switch key: an icon carries less to read than "123"/"ABC".
    static let emoji: CGFloat = 1.4
}

/// The emoji key used when panel access lives in an action row instead of the toolbar.
///
/// Only a layout without a toolbar carries it. The toolbar hides its own emoji button while
/// this key is present, so the two never appear at once; the roles route identically either
/// way.
func actionRowEmojiKey(page: String) -> KeySpec {
    specialKey(
        "emoji-\(page)",
        "",
        .emoji,
        weight: SystemActionRowWeights.emoji,
        accessibilityLabel: "Mở bảng biểu tượng cảm xúc"
    )
}

/// `<switch> space <enter>`, the action row shared by every system-preset page.
func systemActionRow(
    page: String,
    switchID: String,
    switchLabel: String,
    switchRole: KeyRole,
    switchAccessibility: String
) -> KeyboardRow {
    KeyboardRow(
        keys: [
            specialKey(
                "\(switchID)-\(page)",
                switchLabel,
                switchRole,
                accessibilityLabel: switchAccessibility
            ),
            standardSpaceKey(),
            specialKey("enter-\(page)", "", .enter, accessibilityLabel: "Enter"),
        ],
        columnSpans: SystemRowSpans.action
    )
}

/// The action row for the letters page: `123` switches to the symbol pages.
func systemLettersActionRow(page: String) -> KeyboardRow {
    systemActionRow(
        page: page,
        switchID: "action-switch",
        switchLabel: "123",
        switchRole: .symbols,
        switchAccessibility: "Ký hiệu"
    )
}

/// The action row for both symbol pages: `ABC` returns to the letters page.
func systemSymbolsActionRow(page: String) -> KeyboardRow {
    systemActionRow(
        page: page,
        switchID: "action-switch",
        switchLabel: "ABC",
        switchRole: .letters,
        switchAccessibility: "Chữ cái"
    )
}

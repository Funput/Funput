import Foundation

/// Weights of the system action row, `<switch> emoji space <enter>`.
///
/// They are chosen so the spacebar sits centred on screen, as it does on the stock
/// keyboard. Two keys and two gaps stand to its left but only one key and one gap to
/// its right, so the enter key has to span the switch key, the emoji key *and* the gap
/// between them. `KeyboardGeometry` sizes gaps in points rather than weight, so that
/// last part is the `gapWeight` correction below: one 5pt gap over the ~32.4pt a unit
/// of weight buys on a 390pt phone. It varies by a fraction of a point across device
/// widths, which is far below what an eye or a thumb can tell.
enum SystemActionRowWeights {
    static let `switch`: CGFloat = 1.7
    static let emoji: CGFloat = 1.5
    private static let gap: CGFloat = 0.15

    static let enter = `switch` + emoji + gap
    /// The row still totals 11.2, matching `standardActionRow`, so a unit of weight buys
    /// the same width in both presets and the switch key keeps the width it has there.
    static let space = 11.2 - `switch` - emoji - enter
}

/// The emoji key the system preset places in its action row.
///
/// It sits between a letter key and the switch key in width — an icon key as narrow as
/// a letter reads as a mis-tap target. The toolbar hides its own emoji button while this
/// key is present, so the two never appear at once; the roles route identically either way.
func systemEmojiKey(page: String) -> KeySpec {
    specialKey(
        "emoji-\(page)",
        "",
        .emoji,
        weight: SystemActionRowWeights.emoji,
        accessibilityLabel: "Mở bảng biểu tượng cảm xúc"
    )
}

/// `<switch> emoji space <enter>`, the action row shared by every system-preset page.
func systemActionRow(
    page: String,
    switchID: String,
    switchLabel: String,
    switchRole: KeyRole,
    switchAccessibility: String
) -> KeyboardRow {
    KeyboardRow(keys: [
        specialKey(
            "\(switchID)-\(page)",
            switchLabel,
            switchRole,
            weight: SystemActionRowWeights.switch,
            accessibilityLabel: switchAccessibility
        ),
        systemEmojiKey(page: page),
        standardSpaceKey(weight: SystemActionRowWeights.space),
        specialKey(
            "enter-\(page)",
            "",
            .enter,
            weight: SystemActionRowWeights.enter,
            accessibilityLabel: "Enter"
        ),
    ])
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

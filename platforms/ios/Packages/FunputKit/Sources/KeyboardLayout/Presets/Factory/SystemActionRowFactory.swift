import Foundation

/// Width weights for action-row keys that are not placed on the grid.
enum SystemActionRowWeights {
    /// Narrower than a switch key: an icon carries less to read than "123"/"ABC".
    static let emoji: CGFloat = 1.4
}

/// The emoji key used when panel access lives in an action row instead of the toolbar.
///
/// It matches the switch key beside it, as on the stock keyboard. The toolbar hides its own
/// emoji button while this key is present, so the two never appear at once; the roles route
/// identically either way.
func actionRowEmojiKey(page: String) -> KeySpec {
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
    KeyboardRow(
        keys: [
            specialKey(
                "\(switchID)-\(page)",
                switchLabel,
                switchRole,
                accessibilityLabel: switchAccessibility
            ),
            actionRowEmojiKey(page: page),
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

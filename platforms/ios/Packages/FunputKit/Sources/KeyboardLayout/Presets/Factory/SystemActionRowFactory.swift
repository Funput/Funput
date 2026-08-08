import Foundation

/// The emoji key the system preset places in its action row.
///
/// It carries the same weight as the switch key beside it, which is how the stock
/// keyboard sizes it — an icon key at the width of a letter reads as a mis-tap target.
/// The toolbar hides its own emoji button while this key is present, so the two never
/// appear at once; the roles are routed identically either way.
func systemEmojiKey(page: String) -> KeySpec {
    specialKey(
        "emoji-\(page)",
        "",
        .emoji,
        weight: 1.7,
        accessibilityLabel: "Mở bảng biểu tượng cảm xúc"
    )
}

/// `<switch> emoji space <enter>`, the action row shared by every system-preset page.
///
/// The weights total 11.2, matching `standardActionRow`, so the switch and enter keys
/// render at exactly the width they have under the Funput preset — the space taken by
/// the comma and period keys is absorbed by the spacebar rather than redistributed.
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
            weight: 1.7,
            accessibilityLabel: switchAccessibility
        ),
        systemEmojiKey(page: page),
        standardSpaceKey(weight: 6.1),
        specialKey("enter-\(page)", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
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

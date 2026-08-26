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
    /// Sized to render at the same width as the Shift key above it.
    ///
    /// Equal weight would *not* do that: the bottom letter row is nine keys and eight
    /// gaps totalling weight 10, this row is four keys and three gaps totalling 11.2, so
    /// a unit of weight buys a different width in each. Solving the two for equal points
    /// gives 1.54 on a 320pt phone and 1.58 on a 430pt one; 1.56 splits the difference
    /// and lands within half a point everywhere.
    static let `switch`: CGFloat = 1.56

    /// Narrower than the switch key: an icon carries less to read than "123"/"ABC", and
    /// the width is better spent on the spacebar.
    static let emoji: CGFloat = 1.4

    /// Deliberately below `switch + emoji + gap`, the width that would centre the
    /// spacebar exactly. Trading roughly 8pt of centring for 8pt of spacebar was a
    /// judgement call about which matters more to the thumb; the spacebar therefore sits
    /// a little right of centre. `SystemLettersParityTests.spacebarNearCentre` pins how
    /// far, so a future change cannot drift further without saying so.
    static let enter: CGFloat = 2.6

    /// The row totals 11.2, matching `standardActionRow`, so a unit of weight buys the
    /// same width in both presets and the two remain comparable.
    static let space = 11.2 - `switch` - emoji - enter
}

/// The emoji key used when panel access lives in an action row instead of the toolbar.
///
/// It matches the switch key beside it, as on the stock keyboard. The toolbar hides its
/// own emoji button while this key is present, so the two never appear at once; the
/// roles route identically either way.
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
    KeyboardRow(keys: [
        specialKey(
            "\(switchID)-\(page)",
            switchLabel,
            switchRole,
            weight: SystemActionRowWeights.switch,
            accessibilityLabel: switchAccessibility
        ),
        actionRowEmojiKey(page: page),
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

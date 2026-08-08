import CoreGraphics
import KeyboardLayout
import Testing

struct SystemLettersParityTests {
    @Test("VNI always keeps the number row", arguments: [true, false])
    func vniRowCount(showsNumberRow: Bool) {
        let layout = SystemKeyboardLayouts.letters(.vni, showsNumberRow: showsNumberRow)
        #expect(layout.rows.count == 5)
        #expect(layout.rows[0].keys.map(\.label).joined() == "1234567890")
        #expect(layout.rows[0].keys.allSatisfy { $0.role == .vniModifier })
        #expect(layout.rows[0].keys.compactMap(\.secondaryLabel).count == 10)
    }

    @Test("Telex follows the number row preference", arguments: [true, false])
    func telexRowCount(showsNumberRow: Bool) {
        let layout = SystemKeyboardLayouts.letters(.telex, showsNumberRow: showsNumberRow)
        #expect(layout.rows.count == (showsNumberRow ? 5 : 4))
    }

    @Test("The QWERTY block is untouched", arguments: KeyboardInputMethod.allCases)
    func qwertyRowsMatchFunput(method: KeyboardInputMethod) {
        // Only the action row differs between the presets; if these drift apart, the
        // system preset has started re-implementing what `qwertyLayout` already gives it.
        let system = SystemKeyboardLayouts.letters(method).rows
        let funput = StandardKeyboardLayouts.letters(method).rows
        for index in 1...3 {
            #expect(system[index].keys == funput[index].keys)
            #expect(system[index].horizontalInsetUnits == funput[index].horizontalInsetUnits)
        }
    }

    @Test("The action row drops comma and period for an emoji key", arguments: KeyboardInputMethod.allCases)
    func actionRow(method: KeyboardInputMethod) {
        let keys = SystemKeyboardLayouts.letters(method).rows.last?.keys ?? []
        #expect(keys.map(\.label) == ["123", "", "Tiếng Việt", ""])
        #expect(keys.map(\.role) == [.symbols, .emoji, .space, .enter])
        // Compared with a tolerance: the spacebar and enter weights are derived, so an
        // exact literal match fails on the last binary digit.
        #expect(Self.matches(keys.map(\.widthWeight), [1.4, 1.4, 5.45, 2.95]))
        // The row totals what `standardActionRow` does, so a unit of weight buys the same
        // width in both presets and these numbers stay comparable across them.
        #expect(abs(keys.map(\.widthWeight).reduce(0, +) - 11.2) < 0.001)
        // Enter spans the switch and emoji keys plus the gap between them, which is what
        // puts the spacebar in the middle. See `centredSpacebar`.
        #expect(keys[3].widthWeight > keys[0].widthWeight + keys[1].widthWeight)
        #expect(keys[2].horizontalSwipeAction == .toggleLanguage)
    }

    /// Shared by the action-row weight checks; see the comment at the first call site.
    static func matches(_ weights: [CGFloat], _ expected: [CGFloat]) -> Bool {
        weights.count == expected.count
            && zip(weights, expected).allSatisfy { abs($0 - $1) < 0.001 }
    }

    @Test("The spacebar sits centred on screen", arguments: [320.0, 390.0, 430.0])
    func centredSpacebar(width: Double) {
        // The point of the enter key's width: two keys and two gaps sit left of the
        // spacebar but only one key and one gap right of it, so without the correction
        // the spacebar drifts left of centre — the thing that reads as "not iOS".
        let layout = SystemKeyboardLayouts.letters(.vni)
        let resolved = KeyboardGeometry.resolve(
            layout: layout,
            size: CGSize(width: width, height: 304),
            sizing: .default
        )
        let space = resolved.rows.last?.first { $0.spec.role == .space }?.frame ?? .zero
        #expect(abs(space.midX - width / 2) <= 1)
    }

    @Test("Telex hints survive the preset", arguments: [KeyboardInputMethod.telex, .telexAdvanced])
    func telexHints(method: KeyboardInputMethod) {
        let keys = SystemKeyboardLayouts.letters(method).rows.flatMap(\.keys)
        let hinted = Dictionary(uniqueKeysWithValues: keys.compactMap { key in
            key.secondaryLabel.map { (key.label, $0) }
        })
        #expect(hinted == ["s": "´", "f": "`", "r": "̉", "x": "˜", "j": "̣", "z": "×"])
    }

    @Test("The row emoji key does not shadow the toolbar one", arguments: KeyboardInputMethod.allCases)
    func emojiKeyIsDistinct(method: KeyboardInputMethod) {
        let layout = SystemKeyboardLayouts.letters(method)
        let rowEmoji = layout.rows.flatMap(\.keys).filter { $0.role == .emoji }
        #expect(rowEmoji.count == 1)
        // The spec still carries a toolbar emoji key; the renderer hides its button
        // while a row provides one. The labels stay distinct so the two never read
        // alike should both ever be on screen.
        #expect(layout.toolbar?.keys.map(\.role) == [.clipboard, .emoji])
        let toolbarEmoji = layout.toolbar?.keys.first { $0.role == .emoji }
        #expect(rowEmoji.first?.accessibilityLabel != toolbarEmoji?.accessibilityLabel)
    }

    @Test("Each preset resolves to its own layout identity", arguments: KeyboardInputMethod.allCases)
    func presetsHaveDistinctIDs(method: KeyboardInputMethod) {
        // The renderer rebuilds on layout inequality, so flipping the preset must change
        // the id — otherwise the keys would not be rebuilt.
        for showsNumberRow in [true, false] {
            for mode in KeyboardLayoutMode.allCases {
                let system = KeyboardLayoutResolver.resolve(
                    inputMethod: method,
                    mode: mode,
                    showsNumberRow: showsNumberRow,
                    preset: .system
                )
                let funput = KeyboardLayoutResolver.resolve(
                    inputMethod: method,
                    mode: mode,
                    showsNumberRow: showsNumberRow,
                    preset: .funput
                )
                #expect(system.id != funput.id)
            }
        }
    }
}

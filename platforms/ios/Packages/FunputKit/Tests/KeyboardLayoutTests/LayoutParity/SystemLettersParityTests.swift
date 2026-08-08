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
        #expect(keys.map(\.widthWeight) == [1.7, 1, 6.8, 1.7])
        // Matching `standardActionRow`'s total keeps the switch and enter keys the same
        // width in both presets; the comma and period width goes to the spacebar.
        #expect(keys.map(\.widthWeight).reduce(0, +) == 11.2)
        #expect(keys[2].horizontalSwipeAction == .toggleLanguage)
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
        #expect(layout.toolbar?.keys.map(\.role) == [.clipboard, .emoji])
        // Two ways to the same panel is fine; two identical VoiceOver labels is not.
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

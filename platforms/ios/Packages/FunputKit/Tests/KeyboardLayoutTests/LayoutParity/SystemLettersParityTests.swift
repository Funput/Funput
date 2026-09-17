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

    @Test("The action row is 123, space and enter", arguments: KeyboardInputMethod.allCases)
    func actionRow(method: KeyboardInputMethod) {
        let row = SystemKeyboardLayouts.letters(method).rows.last
        let keys = row?.keys ?? []
        #expect(keys.map(\.label) == ["123", "Tiếng Việt", ""])
        #expect(keys.map(\.role) == [.symbols, .space, .enter])
        #expect(row?.columnSpans?.count == 3)
        #expect(keys[1].horizontalSwipeAction == .toggleLanguage)
    }

    @Test("The spacebar is centred", arguments: [320.0, 390.0, 440.0])
    func spacebarCentred(width: Double) {
        // Apple's spacebar runs from under `x` to the end of `n`, which is symmetric about
        // the middle of the letter grid.
        let resolved = KeyboardGeometry.resolve(
            layout: SystemKeyboardLayouts.letters(.vni),
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

    @Test("Emoji moves into the action row only without a toolbar", arguments: KeyboardInputMethod.allCases)
    func emojiOnlyWithoutToolbar(method: KeyboardInputMethod) {
        let withToolbar = KeyboardLayoutResolver.resolve(inputMethod: method, mode: .letters, preset: .system)
        #expect(!withToolbar.rows.flatMap(\.keys).contains { $0.role == .emoji })
        #expect(withToolbar.toolbar?.keys.map(\.role) == [.clipboard, .emoji])

        let toolbarless = KeyboardLayoutResolver.resolve(
            inputMethod: method,
            mode: .letters,
            preset: .system,
            showsToolbar: false
        )
        let action = toolbarless.rows.last
        #expect(action?.keys.map(\.role) == [.symbols, .emoji, .space, .enter])
        #expect(action?.columnSpans?.count == 4)

        // The emoji key comes out of the switch key; the spacebar does not move.
        let size = CGSize(width: 402, height: 260)
        func spaceFrame(_ layout: KeyboardLayout) -> CGRect? {
            KeyboardGeometry.resolve(layout: layout, size: size, sizing: .system)
                .rows.last?.first { $0.spec.role == .space }?.frame
        }
        #expect(spaceFrame(withToolbar)?.minX == spaceFrame(toolbarless)?.minX)
        #expect(spaceFrame(withToolbar)?.width == spaceFrame(toolbarless)?.width)
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

import KeyboardLayout
import Testing

@Suite("Compact digit hints")
struct CompactDigitAlternatesTests {
    @Test("Top row prints its digit, beside a Telex hint where there is one")
    func hints() throws {
        let compact = Self.compactRow(.telex)
        let standard = Self.topRow(StandardKeyboardLayouts.letters(.telex))
        for (index, key) in compact.keys.enumerated() {
            let tone = standard.keys[index].secondaryLabel
            #expect(key.secondaryLabel == tone.map { "\($0) \(Self.digits[index])" }
                ?? Self.digits[index])
        }
        // `r` is the hỏi key: the tone hint leads and the digit follows it.
        let hint = try #require(compact.keys[3].secondaryLabel)
        let tone = try #require(standard.keys[3].secondaryLabel)
        #expect(hint.hasPrefix(tone))
        #expect(hint.hasSuffix("4"))
    }

    @Test("The digit leads every top-row palette", arguments: [
        KeyboardInputMethod.telex,
        .telexAdvanced,
    ])
    func digitLeadsPalette(method: KeyboardInputMethod) {
        let row = Self.compactRow(method)
        for (index, key) in row.keys.enumerated() {
            #expect(key.alternates.first?.text == Self.digits[index])
            #expect(key.alternates.first?.text(for: .uppercase) == Self.digits[index])
            #expect(key.accessibilityLabel.hasSuffix(", số \(Self.digits[index])"))
        }
        #expect(row.keys[6].alternates.map(\.text)
            == ["7"] + VietnameseKeyAlternates.values(for: "u").map(\.text))
        #expect(row.keys[0].alternates.map(\.text) == ["1"])
    }

    @Test("Keys keep their identity so geometry and layout ids do not move")
    func identity() {
        let layout = StandardKeyboardLayouts.letters(.telex, showsNumberRow: false)
        let row = Self.topRow(layout)
        #expect(layout.id == "qwerty-telex-compact")
        #expect(row.keys.map(\.id) == "qwertyuiop".map { "character-\($0)" })
        #expect(row.keys.allSatisfy { $0.role == .character && $0.widthWeight == 1 })
        #expect(row.keys.map(\.shiftedLabel) == "QWERTYUIOP".map(String.init))
    }

    @Test("Digits stay off whenever a number row is on screen")
    func numberRowWins() {
        for method in KeyboardInputMethod.allCases {
            let row = Self.topRow(StandardKeyboardLayouts.letters(method))
            #expect(row.keys[0].secondaryLabel == nil)
            #expect(row.keys[0].alternates.isEmpty)
            #expect(row.keys[6].alternates.first?.text == "u")
        }
        // VNI forces its own digit row, so the preference never reaches this page.
        let vni = Self.topRow(StandardKeyboardLayouts.letters(.vni, showsNumberRow: false))
        #expect(vni.keys[0].secondaryLabel == nil)
        #expect(vni.keys[0].alternates.isEmpty)
    }

    @Test("Only the compact letters page changes")
    func scope() throws {
        let compact = StandardKeyboardLayouts.letters(.telex, showsNumberRow: false)
        let standard = StandardKeyboardLayouts.letters(.telex)
        for label in ["a", "s", "z"] {
            let key = try #require(compact.rows.flatMap(\.keys).first { $0.label == label })
            let reference = try #require(standard.rows.flatMap(\.keys).first { $0.label == label })
            #expect(key.secondaryLabel == reference.secondaryLabel)
            #expect(key.accessibilityLabel == reference.accessibilityLabel)
            #expect(key.alternates == reference.alternates)
        }
        for mode in [KeyboardEditorMode.search, .email, .url] {
            let row = Self.topRow(
                EditorKeyboardLayouts.resolve(.telex, editorMode: mode, showsNumberRow: false)
            )
            #expect(row.keys[0].secondaryLabel == nil)
            #expect(row.keys[0].alternates.isEmpty)
        }
        let system = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            showsNumberRow: false,
            preset: .system
        )
        #expect(Self.topRow(system).keys[0].secondaryLabel == nil)
        #expect(Self.topRow(system).keys[0].alternates.isEmpty)
    }

    private static let digits = "1234567890".map(String.init)

    private static func compactRow(_ method: KeyboardInputMethod) -> KeyboardRow {
        topRow(StandardKeyboardLayouts.letters(method, showsNumberRow: false))
    }

    private static func topRow(_ layout: KeyboardLayout) -> KeyboardRow {
        layout.rows.first { $0.keys.map(\.label) == "qwertyuiop".map(String.init) }!
    }
}

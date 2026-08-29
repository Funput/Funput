import KeyboardLayout
import Testing

/// Search, email and URL answer the number row preference exactly as the letters page does.
@Suite("Compact web pages")
struct CompactWebPageTests {
    static let modes: [KeyboardEditorMode] = [.search, .email, .url]

    @Test("Telex drops the number row and keeps the digits a long press away", arguments: modes)
    func compact(mode: KeyboardEditorMode) throws {
        for method in [KeyboardInputMethod.telex, .telexAdvanced] {
            let layout = Self.resolve(mode, method: method, showsNumberRow: false)
            #expect(layout.id == "qwerty-\(mode.rawValue)-\(method.rawValue)-compact")
            #expect(layout.rows.count == 4)
            #expect(!layout.rows.contains { $0.keys.map(\.label).joined() == "1234567890" })

            let row = try #require(Self.topRow(layout))
            #expect(row.keys.map { $0.alternates.first?.text } == Self.digits)
            #expect(row.keys.allSatisfy { $0.secondaryLabel?.hasSuffix($0.alternates[0].text) == true })
        }
    }

    @Test("A hidden row does not change what the page says about tones", arguments: modes)
    func hints(mode: KeyboardEditorMode) throws {
        let compact = try #require(Self.topRow(Self.resolve(mode, showsNumberRow: false)))
        let standard = try #require(Self.topRow(Self.resolve(mode, showsNumberRow: true)))
        for (index, key) in compact.keys.enumerated() {
            // Search composes, so its tone hint keeps its slot and the digit joins it;
            // email and URL never had one, so the digit stands alone.
            let tone = standard.keys[index].secondaryLabel
            #expect(key.secondaryLabel == tone.map { "\($0) \(Self.digits[index])" }
                ?? Self.digits[index])
        }
        #expect(mode.supportsVietnameseComposition == (standard.keys[3].secondaryLabel != nil))
    }

    @Test("VNI keeps its digit row whatever the preference is", arguments: modes)
    func vni(mode: KeyboardEditorMode) throws {
        let layout = Self.resolve(mode, method: .vni, showsNumberRow: false)
        #expect(layout.id == "qwerty-\(mode.rawValue)-vni")
        #expect(layout.rows.count == 5)
        #expect(layout.rows[0].keys.map(\.label).joined() == "1234567890")
        // Only a composing page spends that row on tone modifiers.
        let role: KeyRole = mode.supportsVietnameseComposition ? .vniModifier : .character
        #expect(layout.rows[0].keys.allSatisfy { $0.role == role })
        let row = try #require(Self.topRow(layout))
        #expect(!row.keys[0].alternates.contains { $0.text == "1" })
    }

    @Test("The symbols page still shows the digits, in the same panel height", arguments: modes)
    func symbols(mode: KeyboardEditorMode) {
        let letters = Self.resolve(mode, showsNumberRow: false)
        let symbols = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .symbolsPrimary,
            editorMode: mode,
            showsNumberRow: false
        )
        #expect(symbols.rows.count == letters.rows.count)
        #expect(symbols.rows[0].keys.map(\.label).joined() == "1234567890")
    }

    @Test("The preference on leaves every page exactly as it was", arguments: modes)
    func preferenceOn(mode: KeyboardEditorMode) {
        for method in KeyboardInputMethod.allCases {
            let layout = Self.resolve(mode, method: method, showsNumberRow: true)
            #expect(layout.id == "qwerty-\(mode.rawValue)-\(method.rawValue)")
            #expect(layout.rows.count == 5)
            #expect(layout.rows[0].keys.map(\.label).joined() == "1234567890")
        }
    }

    private static let digits = "1234567890".map(String.init)

    private static func resolve(
        _ mode: KeyboardEditorMode,
        method: KeyboardInputMethod = .telex,
        showsNumberRow: Bool
    ) -> KeyboardLayout {
        EditorKeyboardLayouts.resolve(method, editorMode: mode, showsNumberRow: showsNumberRow)
    }

    private static func topRow(_ layout: KeyboardLayout) -> KeyboardRow? {
        layout.rows.first { $0.keys.map(\.label) == "qwertyuiop".map(String.init) }
    }
}

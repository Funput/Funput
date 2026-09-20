import KeyboardLayout
import Testing

/// What the space bar becomes once the language switch is off.
struct LanguageToggleLayoutTests {
    @Test(
        "Every page drops the switch together",
        arguments: KeyboardLayoutMode.allCases, KeyboardLayoutPreset.allCases
    )
    func spaceBarsLoseTheSwitch(mode: KeyboardLayoutMode, preset: KeyboardLayoutPreset) {
        let switched = resolve(mode: mode, preset: preset, allowsToggle: true)
        let plain = resolve(mode: mode, preset: preset, allowsToggle: false)

        // The switch rides on the space key of the letters page and both symbol pages, so
        // leaving one behind would strand the user in English on that page alone.
        #expect(spaceKeys(switched).contains { $0.horizontalSwipeAction == .toggleLanguage })
        #expect(spaceKeys(plain).allSatisfy { $0.horizontalSwipeAction == nil })
        for key in spaceKeys(plain) {
            #expect(key.label == "␣")
            #expect(!key.accessibilityLabel.contains("Tiếng Anh"))
        }
    }

    @Test("A locked space bar keeps its size and its place")
    func geometryIsUntouched() {
        let switched = resolve(mode: .letters, preset: .funput, allowsToggle: true)
        let plain = resolve(mode: .letters, preset: .funput, allowsToggle: false)

        #expect(plain.rows.map(\.keys.count) == switched.rows.map(\.keys.count))
        #expect(spaceKeys(plain).map(\.widthWeight) == spaceKeys(switched).map(\.widthWeight))
        #expect(plain.toolbar?.keys.map(\.role) == switched.toolbar?.keys.map(\.role))
    }

    @Test("Secure pages were already plain and pass through")
    func securePagesUnchanged() {
        for mode in KeyboardLayoutMode.allCases {
            let secure = KeyboardLayoutResolver.resolve(
                inputMethod: .telex,
                mode: mode,
                editorMode: .password,
                allowsLanguageToggle: false
            )
            #expect(spaceKeys(secure).allSatisfy { $0.horizontalSwipeAction == nil })
            #expect(spaceKeys(secure).allSatisfy { $0.label != "␣" })
        }
    }

    private func spaceKeys(_ layout: KeyboardLayout) -> [KeySpec] {
        layout.rows.flatMap(\.keys).filter { $0.role == .space }
    }

    private func resolve(
        mode: KeyboardLayoutMode,
        preset: KeyboardLayoutPreset,
        allowsToggle: Bool
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: mode,
            preset: preset,
            allowsLanguageToggle: allowsToggle
        )
    }
}

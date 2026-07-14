import KeyboardLayout
import Testing

struct EditorLayoutParityTests {
    @Test("Every editor mode resolves for both input methods")
    func completeMatrix() {
        for method in KeyboardInputMethod.allCases {
            for mode in KeyboardEditorMode.allCases {
                let layout = KeyboardLayoutResolver.resolve(
                    inputMethod: method,
                    mode: .letters,
                    editorMode: mode
                )
                #expect(!layout.rows.isEmpty)
                let keys = layout.rows.flatMap(\.keys) + (layout.toolbar?.keys ?? [])
                #expect(Set(keys.map(\.id)).count == keys.count)
            }
        }
    }

    @Test("Email layout matches Android", arguments: KeyboardInputMethod.allCases)
    func email(method: KeyboardInputMethod) {
        assertWebContract(resolve(.email, method: method), middleKey: "@", supportsLanguageSwipe: false)
    }

    @Test("Search preserves web keys and supports Vietnamese", arguments: KeyboardInputMethod.allCases)
    func search(method: KeyboardInputMethod) {
        assertWebContract(resolve(.search, method: method), middleKey: "/", supportsLanguageSwipe: true)
    }

    @Test("URL preserves English web input", arguments: KeyboardInputMethod.allCases)
    func url(method: KeyboardInputMethod) {
        assertWebContract(resolve(.url, method: method), middleKey: "/", supportsLanguageSwipe: false)
    }

    @Test("Search and URL keep identical geometry")
    func webGeometry() {
        for method in KeyboardInputMethod.allCases {
            let search = resolve(.search, method: method).rows[4].keys
            let url = resolve(.url, method: method).rows[4].keys
            #expect(search.map(\.widthWeight) == url.map(\.widthWeight))
        }
    }

    @Test("Password is secure ASCII QWERTY", arguments: KeyboardInputMethod.allCases)
    func password(method: KeyboardInputMethod) {
        let layout = resolve(.password, method: method)
        #expect(layout.toolbar == nil)
        #expect(layout.rows[0].keys.map(\.label).joined() == "1234567890")
        #expect(!layout.rows.flatMap(\.keys).contains { $0.role == .vniModifier })
        let action = layout.rows[4].keys
        #expect(action.map(\.label) == ["?123", ",", "English", ".", ""])
        #expect(action.map(\.role) == [.symbols, .punctuation, .space, .punctuation, .enter])
        #expect(action.map(\.widthWeight) == [1.7, 1, 5.8, 1, 1.7])
        #expect(space(layout).label == "English")
        #expect(space(layout).horizontalSwipeAction == nil)
    }

    private func resolve(
        _ mode: KeyboardEditorMode,
        method: KeyboardInputMethod = .vni
    ) -> KeyboardLayout {
        KeyboardLayoutResolver.resolve(inputMethod: method, mode: .letters, editorMode: mode)
    }

    private func assertWebContract(
        _ layout: KeyboardLayout,
        middleKey: String,
        supportsLanguageSwipe: Bool
    ) {
        #expect(layout.rows.count == 5)
        #expect(layout.rows[0].keys.map(\.label).joined() == "1234567890")
        #expect(layout.rows[0].keys.allSatisfy { $0.role == .character && $0.widthWeight == 1 })
        let action = layout.rows[4].keys
        let spaceLabel = supportsLanguageSwipe ? "Tiếng Việt" : "English"
        #expect(action.map(\.label) == ["?123", middleKey, spaceLabel, ".", ".com", ""])
        #expect(action.map(\.role) == [
            .symbols, .punctuation, .space, .punctuation, .punctuation, .enter,
        ])
        #expect(action.map(\.widthWeight) == [1.7, 1.7, 3.7, 1, 1.7, 1.7])
        #expect(space(layout).horizontalSwipeAction == (
            supportsLanguageSwipe ? .toggleLanguage : nil
        ))
    }

    private func space(_ layout: KeyboardLayout) -> KeySpec {
        layout.rows.flatMap(\.keys).first { $0.role == .space }!
    }
}

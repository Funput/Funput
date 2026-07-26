import CoreGraphics
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

    @Test("Email keeps a full-width space", arguments: KeyboardInputMethod.allCases)
    func email(method: KeyboardInputMethod) {
        assertWebContract(resolve(.email, method: method), middleKey: "@", supportsLanguageSwipe: false)
    }

    @Test("Search keeps a full-width space and supports Vietnamese", arguments: KeyboardInputMethod.allCases)
    func search(method: KeyboardInputMethod) {
        assertWebContract(resolve(.search, method: method), middleKey: "/", supportsLanguageSwipe: true)
    }

    @Test("URL keeps a full-width space with English web input", arguments: KeyboardInputMethod.allCases)
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

    @Test("Specialized QWERTY spaces match text geometry")
    func fullWidthSpaceGeometry() {
        let sizes = [
            CGSize(width: 320, height: 238),
            CGSize(width: 390, height: 304),
            CGSize(width: 568, height: 236),
            CGSize(width: 744, height: 324),
        ]
        for method in KeyboardInputMethod.allCases {
            let text = resolve(.text, method: method)
            for mode in [KeyboardEditorMode.search, .email, .url, .password] {
                let specialized = resolve(mode, method: method)
                for size in sizes {
                    #expect(abs(spaceWidth(specialized, size) - spaceWidth(text, size)) <= 0.5)
                }
            }
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

    @Test("Telex hints stay out of specialized QWERTY editors")
    func specializedEditorsHideTelexHints() {
        for mode in [KeyboardEditorMode.search, .email, .url, .password] {
            let keys = resolve(mode, method: .telex).rows.flatMap(\.keys)
            #expect(keys.allSatisfy { $0.secondaryLabel == nil })
        }
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
        #expect(action.map(\.label) == ["?123", middleKey, spaceLabel, ".", ""])
        #expect(action.map(\.role) == [
            .symbols, .punctuation, .space, .punctuation, .enter,
        ])
        #expect(action.map(\.widthWeight) == [1.7, 1, 5.8, 1, 1.7])
        #expect(space(layout).horizontalSwipeAction == (
            supportsLanguageSwipe ? .toggleLanguage : nil
        ))
    }

    private func space(_ layout: KeyboardLayout) -> KeySpec {
        layout.rows.flatMap(\.keys).first { $0.role == .space }!
    }

    private func spaceWidth(_ layout: KeyboardLayout, _ size: CGSize) -> CGFloat {
        KeyboardGeometry.resolve(layout: layout, size: size, sizing: .default)
            .keys.first { $0.spec.role == .space }!.frame.width
    }
}

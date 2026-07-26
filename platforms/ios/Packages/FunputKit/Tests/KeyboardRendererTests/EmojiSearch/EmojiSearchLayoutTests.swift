#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@Suite("Emoji search keyboard layout")
struct EmojiSearchLayoutTests {
    @Test("Uses compact QWERTY without a toolbar")
    func structure() {
        let layout = EmojiSearchKeyboardLayout.layout
        #expect(layout.toolbar == nil)
        #expect(layout.rows.count == 4)
        #expect(layout.rows[0].keys.map(\.label).joined() == "qwertyuiop")
        #expect(layout.rows[2].keys.first?.role == .shift)
        #expect(layout.rows[2].keys.last?.role == .backspace)
        #expect(layout.rows[3].keys.map(\.role) == [.emoji, .space, .enter])
    }
}
#endif

import KeyboardLayout
import Testing

@MainActor
@Suite("Plain local text field")
struct PlainLocalTextComposerTests {
    @Test("Keeps keys as typed and never composes")
    func literal() {
        let field = PlainLocalTextComposer()
        field.apply(.text("a"))
        field.apply(.text("s"))
        #expect(field.text == "as")
    }

    @Test("Drops leading and repeated spaces")
    func spacing() {
        let field = PlainLocalTextComposer()
        let edits: [LocalTextEdit] = [.space, .text("m"), .space, .space, .text("a")]
        edits.forEach { field.apply($0) }
        #expect(field.text == "m a")
    }

    @Test("Deletes one character at a time and resets")
    func deletion() {
        let field = PlainLocalTextComposer()
        field.apply(.deleteBackward)
        field.apply(.text("đỏ"))
        field.apply(.deleteBackward)
        #expect(field.text == "đ")
        field.reset()
        #expect(field.text.isEmpty)
    }
}

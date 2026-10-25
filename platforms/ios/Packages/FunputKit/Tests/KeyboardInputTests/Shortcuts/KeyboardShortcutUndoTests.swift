#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// One Backspace, or the `↩` chip, takes a gõ tắt expansion back and keeps the boundary.
@MainActor
struct KeyboardShortcutUndoTests {
    @Test("Backspace puts the trigger back and keeps the space, then deletes as usual")
    func backspaceUndoes() {
        let fixture = ShortcutUndoFixture()
        fixture.type("vn ")
        #expect(fixture.text == "Việt Nam ")
        #expect(fixture.preview == .shortcut(undoText: "vn"))

        fixture.backspace()
        #expect(fixture.text == "vn ")
        #expect(fixture.preview == nil)

        fixture.backspace()
        #expect(fixture.text == "vn")
    }

    @Test("Punctuation stays with the restored word")
    func punctuationKept() {
        let fixture = ShortcutUndoFixture(shortcuts: ["kg": "không"])
        fixture.type("kg,")
        #expect(fixture.text == "không,")
        fixture.backspace()
        #expect(fixture.text == "kg,")
    }

    @Test("A composed trigger comes back as the word on screen, not as its raw keys")
    func composedTrigger() {
        let fixture = ShortcutUndoFixture(shortcuts: ["đc": "được"])
        fixture.type("ddc ")
        #expect(fixture.text == "được ")
        #expect(fixture.preview == .shortcut(undoText: "đc"))
        fixture.backspace()
        #expect(fixture.text == "đc ")
    }

    @Test("A word English restore would turn back into keys comes back as those keys")
    func englishRestore() {
        let fixture = ShortcutUndoFixture(shortcuts: ["text": "văn bản"], eagerRestore: false)
        fixture.type("text ")
        #expect(fixture.text == "văn bản ")
        fixture.backspace()
        #expect(fixture.text == "text ")
    }

    @Test("It works with the keyboard in English mode")
    func englishMode() {
        let fixture = ShortcutUndoFixture(language: .english)
        fixture.type("vn ")
        #expect(fixture.text == "Việt Nam ")
        #expect(fixture.preview == .shortcut(undoText: "vn"))
        fixture.backspace()
        #expect(fixture.text == "vn ")
    }

    @Test("The chip needs neither autocorrect nor a dictionary")
    func chipIndependentOfCorrection() {
        let fixture = ShortcutUndoFixture(typoCorrection: false)
        fixture.coordinator.allowsAutocorrect = false
        fixture.type("vn ")
        #expect(fixture.preview == .shortcut(undoText: "vn"))
    }

    @Test("Tapping the chip is the same undo")
    func chipTap() {
        let fixture = ShortcutUndoFixture()
        fixture.type("vn ")
        fixture.coordinator.undoShortcutFromBar(writer: fixture.document)
        #expect(fixture.text == "vn ")
        let again = fixture.coordinator.undoShortcutFromBar(writer: fixture.document)
        #expect(again == .none)
        #expect(fixture.text == "vn ")
    }

    @Test("The next key spends the undo, and Backspace then deletes one character")
    func nextKeySpends() {
        let fixture = ShortcutUndoFixture()
        fixture.type("vn x")
        #expect(fixture.preview == nil)
        fixture.backspace()
        #expect(fixture.text == "Việt Nam ")
    }

    @Test("A document that moved on is left alone")
    func movedDocument() {
        let fixture = ShortcutUndoFixture()
        fixture.type("vn ")
        fixture.document.replaceTextExternally(with: "khác ")
        _ = fixture.coordinator.synchronizeDocument(fixture.document, event: .textChanged)
        fixture.backspace()
        #expect(fixture.text == "khác")
    }

    @Test("Taking an expansion back does not re-open the word before it")
    func doesNotReopen() {
        let fixture = ShortcutUndoFixture(shortcuts: ["a": "anh"])
        fixture.type("a ")
        #expect(fixture.text == "anh ")
        fixture.backspace()
        #expect(fixture.text == "a ")
        #expect(fixture.coordinator.composer.buffer().isEmpty)
    }

    @Test("Without a table there is nothing to take back")
    func noTable() {
        let fixture = ShortcutUndoFixture(shortcuts: [:])
        fixture.type("vn ")
        #expect(fixture.text == "vn ")
        #expect(fixture.preview == nil)
    }
}
#endif

#if os(iOS) && canImport(FunputCore)
import FunputEngine
import Testing

/// The three calls the keyboard makes to take a gõ tắt expansion back, against the real engine.
@MainActor
struct FunputShortcutUndoTests {
    private func composer(trigger: String = "vn", expansion: String = "Việt Nam") -> FunputComposer {
        let composer = FunputComposer()
        composer.setInputMethod(.telex)
        composer.addShortcut(trigger: trigger, expansion: expansion)
        return composer
    }

    private func type(_ text: String, into composer: FunputComposer) {
        for scalar in text.unicodeScalars { composer.process(scalar) }
    }

    @Test("The undo names the word, takes the expansion back once, then is spent")
    func roundTrip() {
        let composer = composer()
        #expect(composer.shortcutUndoText == nil)

        type("vn ", into: composer)
        #expect(composer.shortcutUndoText == "vn")
        #expect(composer.shortcutUndoText == "vn", "asking does not spend it")

        let result = composer.undoShortcut()
        #expect(result.action == .send)
        #expect(result.deleteCount == "Việt Nam ".unicodeScalars.count)
        #expect(result.text == "vn ")

        #expect(composer.shortcutUndoText == nil)
        #expect(composer.undoShortcut().action == .none)
    }

    @Test("A word longer than the result array arrives whole")
    func longWord() {
        let trigger = String(repeating: "b", count: 200)
        let composer = composer(trigger: trigger, expansion: "x")
        type("\(trigger) ", into: composer)

        #expect(composer.shortcutUndoText == trigger)
        #expect(composer.undoShortcut().text == "\(trigger) ")
    }

    @Test("Dismissing drops the undo")
    func dismiss() {
        let composer = composer()
        type("vn ", into: composer)
        composer.dismissShortcutUndo()

        #expect(composer.shortcutUndoText == nil)
        #expect(composer.undoShortcut().action == .none)
    }

    @Test("Clearing the composer drops the undo too")
    func clear() {
        let composer = composer()
        type("vn ", into: composer)
        composer.clear()

        #expect(composer.shortcutUndoText == nil)
    }
}
#endif

import FunputShared
@testable import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct ShortcutInputTests {
    @Test(arguments: KeyboardInputMethod.allCases)
    func vietnameseAndBoundaries(method: KeyboardInputMethod) {
        let rig = ShortcutTypingRig(language: .vietnamese, method: method)
        rig.type("vn,Vn\nVN ")
        #expect(rig.writer.text == "việt nam,Việt Nam\nVIỆT NAM ")
    }

    @Test func englishOptionsAndExactCase() {
        let rig = ShortcutTypingRig(library: .init(entries: [
            .init(trigger: "vn", expansion: "việt nam"), .init(trigger: "VN", expansion: "other")
        ], smartCase: false))
        rig.type("vn VN Vn ")
        #expect(rig.writer.text == "việt nam other Vn ")
        rig.coordinator.receiveShortcuts(.init(entries: [.init(trigger: "vn", expansion: "new")], inEnglish: false))
        rig.type("vn ")
        #expect(rig.writer.text.hasSuffix("Vn vn "))
        rig.coordinator.toggleLanguage()
        rig.type("vn ")
        #expect(rig.writer.text.hasSuffix("vn new "))
        rig.coordinator.receiveShortcuts(.init(isEnabled: false))
        rig.type("vn ")
        #expect(rig.writer.text.hasSuffix("new vn "))
    }

    @Test func longUnicodeReplacementDoesNotDeletePrefix() {
        let expansion = String(repeating: "dòng 👨‍👩‍👧‍👦\n", count: 100)
        let rig = ShortcutTypingRig(library: .init(entries: [
            .init(trigger: "á👨‍👩‍👧‍👦", expansion: expansion)
        ], smartCase: false))
        rig.type("prefix á👨‍👩‍👧‍👦 ")
        #expect(rig.writer.text == "prefix " + expansion + " ")
        #expect(rig.writer.deletions == 2)
    }

    @Test func backspaceDropsWholeGraphemeFromEnglishBuffer() {
        let rig = ShortcutTypingRig()
        rig.type("vn👨‍👩‍👧‍👦")
        rig.key("delete", role: .backspace)
        rig.type(" ")
        #expect(rig.writer.text == "việt nam ")
    }

    @Test func doubleSpaceAndLiteralInsertion() {
        let rig = ShortcutTypingRig()
        rig.type("vn  ")
        #expect(rig.writer.text == "việt nam. ")
        rig.type("v")
        rig.coordinator.insertLiteral("n", writer: rig.writer)
        rig.type(" ")
        #expect(rig.writer.text == "việt nam. vn ")
    }

    @Test(arguments: KeyboardEditorMode.allCases, KeyboardLanguage.allCases)
    func fieldEligibility(mode: KeyboardEditorMode, language: KeyboardLanguage) {
        let rig = ShortcutTypingRig(language: language)
        rig.coordinator.updateContext(.init(editorMode: mode, enterAction: .newLine, autocapitalization: .none))
        rig.type("vn ")
        let expected = mode == .text || mode == .search ? "việt nam " : "vn "
        #expect(rig.writer.text == expected)
    }

    @Test func advancedTelexBracketsAreNotBoundaries() {
        let rig = ShortcutTypingRig(language: .vietnamese, method: .telexAdvanced)
        rig.key("[", role: .punctuation)
        #expect(rig.writer.text == "ư")
    }

    @Test func unsafeDeletionIsRejected() {
        #expect(KeyboardReplacement.deletionCount(scalars: 1, buffer: "á", context: "á") == nil)
        #expect(KeyboardReplacement.deletionCount(scalars: 2, buffer: "vn", context: "n") == nil)
        #expect(KeyboardReplacement.deletionCount(scalars: 2, buffer: "vn", context: nil) == nil)
        #expect(KeyboardReplacement.deletionCount(scalars: 2, buffer: "vn", context: "other") == nil)
        let rig = ShortcutTypingRig()
        rig.type("prefix v")
        rig.coordinator.abandonUnsafeReplacement()
        rig.type("n vn ")
        #expect(rig.writer.text == "prefix vn việt nam ")
    }

    @Test func backspaceAfterExpansionKeepsOrdinaryDeletion() {
        let rig = ShortcutTypingRig()
        rig.type("vn ")
        rig.key("delete", role: .backspace)
        rig.key("delete", role: .backspace)
        #expect(rig.writer.text == "việt na")
    }

}

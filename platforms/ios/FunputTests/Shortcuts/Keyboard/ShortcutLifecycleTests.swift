import FunputShared
import KeyboardLayout
import KeyboardInput
import Testing

@MainActor
struct ShortcutLifecycleTests {
    @Test func lateLoadWaitsForBoundaryInEnglish() {
        let rig = ShortcutTypingRig()
        rig.coordinator.beginShortcutActivation()
        rig.type("v")
        rig.coordinator.receiveShortcuts(.init(entries: [.init(trigger: "vn", expansion: "new")]))
        rig.type("n vn ")
        #expect(rig.writer.text == "vn new ")
    }

    @Test func lateLoadKeepsVietnameseComposition() {
        let rig = ShortcutTypingRig(language: .vietnamese, method: .telex)
        rig.coordinator.beginShortcutActivation()
        rig.type("cha")
        rig.coordinator.receiveShortcuts(.init(entries: [.init(trigger: "vn", expansion: "new")]))
        rig.type("of vn ")
        #expect(rig.writer.text == "chào new ")
    }

    @Test func lateLoadWaitsForReopenedVietnameseWord() {
        let rig = ShortcutTypingRig(language: .vietnamese, method: .telex)
        rig.coordinator.beginShortcutActivation()
        rig.type("va ")
        rig.key("delete", role: .backspace)
        rig.coordinator.receiveShortcuts(.init(entries: [.init(trigger: "va", expansion: "new")]))
        rig.type(" va ")
        #expect(rig.writer.text == "va new ")
    }

    @Test func newActivationAndEmptyLibraryRemoveOldEntries() {
        let rig = ShortcutTypingRig()
        rig.type("vn ")
        rig.coordinator.beginShortcutActivation()
        rig.type("vn ")
        rig.coordinator.receiveShortcuts(.init())
        rig.type("vn ")
        #expect(rig.writer.text == "việt nam vn vn ")
    }

    @Test func cursorSelectionAndLanguageEndOldTrigger() {
        let rig = ShortcutTypingRig()
        rig.type("v")
        rig.coordinator.moveCursor(by: 1, writer: rig.writer)
        rig.type("n ")
        #expect(rig.writer.text == "vn ")
        rig.type("v")
        rig.writer.hasSelection = true
        rig.coordinator.synchronizeDocument(rig.writer, event: .selectionChanged)
        rig.writer.hasSelection = false
        rig.type("n ")
        #expect(rig.writer.text == "vn vn ")
        rig.type("v")
        rig.coordinator.toggleLanguage()
        rig.type("n ")
        #expect(rig.writer.text == "vn vn vn ")
    }

    @Test func delayedEchoAfterExpansionDoesNotRestoreTrigger() {
        let rig = ShortcutTypingRig()
        rig.type("vn ")
        rig.writer.reportedContext = "vn"
        rig.coordinator.synchronizeDocument(rig.writer, event: .textChanged)
        rig.writer.reportedContext = nil
        rig.type("vn ")
        #expect(rig.writer.text == "việt nam việt nam ")
    }

    @Test func activationDropsAnUnfinishedTrigger() {
        let rig = ShortcutTypingRig()
        rig.type("v")
        rig.coordinator.beginShortcutActivation()
        rig.coordinator.receiveShortcuts(.init(entries: [.init(trigger: "vn", expansion: "new")]))
        rig.type("n vn ")
        #expect(rig.writer.text == "vn new ")
    }

}

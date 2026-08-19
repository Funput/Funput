#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputShared
import KeyboardInput
import KeyboardLayout
import Testing

@MainActor
struct SmartSpaceTests {
    @Test("A second quick space becomes a full stop")
    func punctuates() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")

        space(coordinator, document)
        space(coordinator, document)

        #expect(document.text == "xin chao. ")
    }

    @Test("The next character after the substitution is capitalized")
    func capitalizes() {
        let (coordinator, document) = makeCoordinator(
            context: "xin chao",
            mode: .sentences
        )

        space(coordinator, document)
        space(coordinator, document)

        #expect(coordinator.state.shiftState == .uppercase)
        type("t", with: coordinator, into: document)
        #expect(document.text == "xin chao. T")
    }

    @Test("Two slow spaces stay two spaces")
    func slowTapsAreSpaces() {
        var now: Double = 0
        let (coordinator, document) = makeCoordinator(context: "xin chao") { now }

        space(coordinator, document)
        now = 1
        space(coordinator, document)

        #expect(document.text == "xin chao  ")
    }

    @Test("A key between the spaces breaks the sequence")
    func interveningKeyBreaksSequence() {
        let (coordinator, document) = makeCoordinator(context: "xin")

        space(coordinator, document)
        type("a", with: coordinator, into: document)
        space(coordinator, document)
        space(coordinator, document)

        #expect(document.text == "xin a. ")
    }

    @Test("Nothing is substituted when the gesture setting is off")
    func disabled() {
        let (coordinator, document) = makeCoordinator(context: "xin chao")
        var configuration = FunputConfiguration.default
        configuration.smartGesturesEnabled = false
        coordinator.apply(configuration)
        coordinator.synchronizeDocument(document, event: .activated)

        space(coordinator, document)
        space(coordinator, document)

        #expect(document.text == "xin chao  ")
    }

    @Test("A pending selection is never replaced by a full stop")
    func selectionSuppresses() {
        let (coordinator, document) = makeCoordinator(context: "xin chao ")
        document.hasSelection = true
        coordinator.synchronizeDocument(document, event: .selectionChanged)

        space(coordinator, document)
        space(coordinator, document)

        #expect(document.text.hasSuffix("  "))
    }

    private func space(
        _ coordinator: KeyboardInputCoordinator,
        _ document: TestKeyboardWriter
    ) {
        coordinator.handle(testKey(.space, label: " "), writer: document)
    }

    private func makeCoordinator(
        context: String,
        mode: KeyboardAutocapitalizationMode = .none,
        clock: @escaping () -> TimeInterval = { 0 }
    ) -> (KeyboardInputCoordinator, TestKeyboardWriter) {
        let coordinator = KeyboardInputCoordinator(gestureClock: clock)
        let document = TestKeyboardWriter()
        document.replaceTextExternally(with: context)
        coordinator.updateContext(inputContext(
            editorMode: .text,
            enterAction: .newLine,
            autocapitalization: mode
        ))
        coordinator.synchronizeDocument(document, event: .activated)
        return (coordinator, document)
    }
}
#endif

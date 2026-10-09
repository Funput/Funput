#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// The emoji view only reports what the search keys pressed; composing is the
/// injected field's job.
@MainActor
@Suite("Emoji search input")
struct EmojiSearchInputTests {
    @Test("Search keys become field edits")
    func edits() {
        let (view, field) = makeView()
        view.beginSearch()
        view.handleSearchKey(event("c", .character))
        view.handleSearchKey(event("7", .vniModifier))
        view.handleSearchKey(event("", .space))
        view.handleSearchKey(event("", .backspace, phase: .repeated))
        view.handleSearchKey(event("x", .character, phase: .pressed))
        #expect(field.edits == [.text("c"), .text("7"), .space, .deleteBackward])
    }

    @Test("Shift cases one letter, then drops back")
    func oneShotShift() {
        let (view, field) = makeView()
        view.beginSearch()
        view.handleSearchKey(event("", .shift))
        view.handleSearchKey(event("d", .character))
        view.handleSearchKey(event("d", .character))
        #expect(field.edits == [.text("D"), .text("d")])
    }

    @Test("Clearing, cancelling and a new field all reset the query")
    func resets() {
        let (view, field) = makeView()
        let before = field.resets
        view.beginSearch()
        view.clearSearch()
        view.resetSearch()
        #expect(field.resets == before + 2)

        view.beginSearch()
        let next = RecordingLocalTextField()
        view.searchComposer = next
        #expect(view.searchState == .browsing)
        #expect(next.resets == 1)
    }

    @Test("Search keyboard follows the document's input method")
    func followsInputMethod() {
        let (view, _) = makeView()
        for method in KeyboardInputMethod.allCases {
            view.apply(
                presentation: KeyboardPresentation(layout: StandardKeyboardLayouts.letters(method)),
                recent: []
            )
            #expect(view.searchKeyboard.presentation.layout
                == PanelSearchKeyboardLayouts.letters(method, spaceLabel: "Tìm emoji"))
        }
    }

    private func makeView() -> (EmojiKeyboardView, RecordingLocalTextField) {
        let view = EmojiKeyboardView(catalog: EmojiCatalog(version: "test", emojis: []))
        let field = RecordingLocalTextField()
        view.searchComposer = field
        return (view, field)
    }

    private func event(
        _ label: String,
        _ role: KeyRole,
        phase: KeyboardKeyEvent.Phase = .released
    ) -> KeyboardKeyEvent {
        KeyboardKeyEvent(
            key: KeySpec(id: "test-\(role.rawValue)", label: label, role: role),
            phase: phase
        )
    }
}

@MainActor
private final class RecordingLocalTextField: LocalTextComposing {
    private(set) var edits: [LocalTextEdit] = []
    private(set) var resets = 0
    var text: String { "" }

    func apply(_ edit: LocalTextEdit) { edits.append(edit) }
    func reset() { resets += 1 }
}
#endif

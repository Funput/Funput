#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout

@MainActor
final class TestKeyboardDocument: KeyboardDocument {
    private(set) var text = ""
    var documentIdentifier = UUID()
    var hasSelection = false
    var exposesContext = true
    var delaysContextUpdates = false
    private var reportedText = ""

    var contextBeforeInput: String? {
        guard exposesContext else { return nil }
        return delaysContextUpdates ? reportedText : text
    }

    func insertText(_ text: String) {
        self.text.append(text)
    }

    func deleteBackward() {
        if !text.isEmpty {
            text.removeLast()
        }
    }

    func replaceTextExternally(with text: String) {
        self.text = text
        reportedText = text
    }

    func publishContext() {
        reportedText = text
    }
}

@MainActor
func type(
    _ text: String,
    role: KeyRole = .character,
    with coordinator: KeyboardInputCoordinator,
    into document: TestKeyboardDocument
) {
    for character in text {
        coordinator.handle(
            KeySpec(
                id: "test-\(role.rawValue)-\(character)",
                label: String(character),
                role: role,
                shiftedLabel: String(character).uppercased()
            ),
            document: document
        )
    }
}

func testKey(_ role: KeyRole, label: String = "") -> KeySpec {
    KeySpec(id: "test-\(role.rawValue)", label: label, role: role)
}

func inputContext(
    editorMode: KeyboardEditorMode,
    enterAction: KeyboardEnterAction,
    initialLayoutMode: KeyboardLayoutMode = .letters,
    autocapitalization: KeyboardAutocapitalizationMode = .none
) -> KeyboardInputContext {
    KeyboardInputContext(
        editorMode: editorMode,
        enterAction: enterAction,
        initialLayoutMode: initialLayoutMode,
        autocapitalization: autocapitalization
    )
}
#endif

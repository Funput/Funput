#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout

@MainActor
final class TestKeyboardWriter: KeyboardDocumentWriting {
    private(set) var text = ""
    private(set) var transactions: [InputTransaction] = []
    var documentIdentifier = UUID()
    var hasSelection = false
    var exposesContext = true
    var delaysContextUpdates = false
    private var reportedText = ""

    var snapshot: KeyboardDocumentSnapshot {
        KeyboardDocumentSnapshot(
            documentIdentifier: documentIdentifier,
            contextBeforeInput: exposesContext
                ? (delaysContextUpdates ? reportedText : text)
                : nil,
            hasSelection: hasSelection
        )
    }

    func apply(_ transaction: InputTransaction) {
        transactions.append(transaction)
        for mutation in transaction.mutations {
            switch mutation {
            case let .deleteBackward(count):
                for _ in 0..<count where !text.isEmpty { text.removeLast() }
            case let .insert(inserted):
                text.append(inserted)
            }
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
    into writer: TestKeyboardWriter
) {
    for character in text {
        coordinator.handle(
            KeySpec(
                id: "test-\(role.rawValue)-\(character)",
                label: String(character),
                role: role,
                shiftedLabel: String(character).uppercased()
            ),
            writer: writer
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

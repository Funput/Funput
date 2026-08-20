#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardInput
import KeyboardLayout

@MainActor
final class TestKeyboardWriter: KeyboardDocumentWriting {
    private(set) var text = ""
    /// Character index of the caret. Defaults to the end of the text on every external
    /// replacement, which is where the pre-trackpad model implicitly kept it.
    private(set) var caret = 0
    /// `text.count` walks the whole string, and the hundred-thousand-key stress test
    /// asks for it on every keystroke. Maintained alongside every mutation instead.
    private var textCount = 0
    private(set) var transactions: [InputTransaction] = []
    /// Optional so tests can reproduce a host that has not bound a document yet.
    var documentIdentifier: UUID? = UUID()
    var hasSelection = false
    var exposesContext = true
    var delaysContextUpdates = false
    private var reportedText = ""

    var snapshot: KeyboardDocumentSnapshot {
        KeyboardDocumentSnapshot(
            documentIdentifier: documentIdentifier,
            contextBeforeInput: exposesContext
                ? (delaysContextUpdates ? reportedText : textBeforeCaret)
                : nil,
            hasSelection: hasSelection
        )
    }

    var textBeforeCaret: String {
        caret == textCount ? text : String(text.prefix(caret))
    }

    func apply(_ transaction: InputTransaction) {
        transactions.append(transaction)
        for mutation in transaction.mutations {
            switch mutation {
            case let .deleteBackward(count):
                for _ in 0..<count where caret > 0 {
                    if caret == textCount {
                        text.removeLast()
                    } else {
                        text.remove(at: text.index(text.startIndex, offsetBy: caret - 1))
                    }
                    caret -= 1
                    textCount -= 1
                }
            case let .insert(inserted):
                // Appending is the overwhelmingly common case and the only one the
                // hundred-thousand-key stress test can afford; indexing is O(n).
                if caret == textCount {
                    text.append(inserted)
                } else {
                    text.insert(
                        contentsOf: inserted,
                        at: text.index(text.startIndex, offsetBy: caret)
                    )
                }
                let insertedCount = inserted.count
                caret += insertedCount
                textCount += insertedCount
            case let .moveCursor(offset):
                caret = min(max(0, caret + offset), textCount)
            }
        }
    }

    func replaceTextExternally(with text: String) {
        self.text = text
        textCount = text.count
        caret = textCount
        reportedText = text
    }

    func publishContext() {
        reportedText = textBeforeCaret
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

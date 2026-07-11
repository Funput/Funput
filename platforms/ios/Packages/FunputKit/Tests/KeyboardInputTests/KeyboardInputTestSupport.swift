#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout

@MainActor
final class TestKeyboardDocument: KeyboardDocument {
    private(set) var text = ""

    func insertText(_ text: String) {
        self.text.append(text)
    }

    func deleteBackward() {
        if !text.isEmpty {
            text.removeLast()
        }
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
#endif

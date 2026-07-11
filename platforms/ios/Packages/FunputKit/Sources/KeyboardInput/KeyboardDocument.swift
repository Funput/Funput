@MainActor
public protocol KeyboardDocument {
    func insertText(_ text: String)
    func deleteBackward()
}

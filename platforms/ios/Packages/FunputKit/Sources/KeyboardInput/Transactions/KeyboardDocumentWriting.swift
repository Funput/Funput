@MainActor
public protocol KeyboardDocumentWriting {
    var snapshot: KeyboardDocumentSnapshot { get }
    /// Text on either side of the caret. Read only by gestures that need the document's
    /// line structure — never on the typing path, unlike ``snapshot``.
    var caretContext: KeyboardCaretContext { get }
    func apply(_ transaction: InputTransaction)
}

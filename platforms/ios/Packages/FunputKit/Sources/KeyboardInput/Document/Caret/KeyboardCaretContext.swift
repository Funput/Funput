/// The text on either side of the caret.
///
/// Read only by the gestures that need the document's line structure. Deliberately kept
/// out of ``KeyboardDocumentSnapshot``: that one is read on every keystroke, where a
/// second proxy round-trip is a cost the typing path cannot absorb.
public struct KeyboardCaretContext: Equatable, Sendable {
    public let before: String
    public let after: String

    public init(before: String, after: String) {
        self.before = before
        self.after = after
    }

    public static let empty = KeyboardCaretContext(before: "", after: "")
}

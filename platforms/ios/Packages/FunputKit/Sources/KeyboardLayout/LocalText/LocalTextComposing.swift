/// One edit a panel's keys make to a ``LocalTextComposing`` field.
public enum LocalTextEdit: Equatable, Sendable {
    /// Text from a character, VNI modifier or punctuation key, already cased by Shift.
    case text(String)
    case space
    case deleteBackward
}

/// A text field owned by the keyboard itself — the emoji search today — edited by a
/// panel's own keys instead of the host document.
///
/// The panel only reports what was pressed; whether that composes Vietnamese is up to
/// the conformer, so the renderer never needs to know about the engine.
@MainActor
public protocol LocalTextComposing: AnyObject {
    var text: String { get }
    func apply(_ edit: LocalTextEdit)
    func reset()
}

/// The spacing rule every local field shares: a query never starts with a space and
/// never holds two in a row.
public enum LocalTextSpacing {
    public static func accepts(after text: String) -> Bool {
        guard let last = text.last else { return false }
        return !last.isWhitespace
    }
}

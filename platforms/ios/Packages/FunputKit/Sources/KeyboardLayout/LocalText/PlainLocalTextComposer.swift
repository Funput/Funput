/// A ``LocalTextComposing`` field that keeps every key exactly as typed.
///
/// The default for panels built without an engine — previews, renderer tests — and the
/// behavior an engine-backed field falls back to while Vietnamese is switched off.
@MainActor
public final class PlainLocalTextComposer: LocalTextComposing {
    public private(set) var text = ""

    public init() {}

    public func apply(_ edit: LocalTextEdit) {
        switch edit {
        case let .text(value):
            text.append(contentsOf: value)
        case .space:
            if LocalTextSpacing.accepts(after: text) { text.append(" ") }
        case .deleteBackward:
            if !text.isEmpty { text.removeLast() }
        }
    }

    public func reset() {
        text = ""
    }
}

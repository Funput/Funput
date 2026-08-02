import Foundation

/// One row of clipboard history, as the keyboard surface sees it.
///
/// A renderer-level mirror of `FunputShared.ClipboardItem` on purpose:
/// `KeyboardRenderer` does not depend on `FunputShared`, and pulling persistence
/// into the view layer to save four lines of mapping would be a bad trade. The
/// controller maps between them, exactly as it does for ``KeyboardClipboardHint``.
public struct KeyboardClipboardEntry: Equatable, Identifiable, Sendable {
    public let id: UUID
    public let text: String
    public let capturedAt: Date
    public let isPinned: Bool

    public init(id: UUID, text: String, capturedAt: Date, isPinned: Bool) {
        self.id = id
        self.text = text
        self.capturedAt = capturedAt
        self.isPinned = isPinned
    }
}

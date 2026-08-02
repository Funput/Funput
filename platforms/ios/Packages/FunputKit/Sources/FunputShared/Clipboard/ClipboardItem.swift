import Foundation

/// One captured clipboard entry.
///
/// `sourceChangeCount` is the `UIPasteboard.changeCount` the text came from. It is
/// what lets the keyboard tell an already-captured item from a genuinely new one
/// without ever reading the pasteboard's contents again.
public struct ClipboardItem: Codable, Equatable, Identifiable, Sendable {
    public let id: UUID
    public let text: String
    public let capturedAt: Date
    public var isPinned: Bool
    public let sourceChangeCount: Int

    public init(
        id: UUID = UUID(),
        text: String,
        capturedAt: Date = Date(),
        isPinned: Bool = false,
        sourceChangeCount: Int
    ) {
        self.id = id
        self.text = text
        self.capturedAt = capturedAt
        self.isPinned = isPinned
        self.sourceChangeCount = sourceChangeCount
    }
}

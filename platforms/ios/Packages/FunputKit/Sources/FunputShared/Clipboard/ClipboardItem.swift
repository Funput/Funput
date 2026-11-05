import Foundation

/// One clipboard history entry: text the user pasted through Funput.
///
/// `sourceChangeCount` is the `UIPasteboard.changeCount` the text came from, or -1
/// when the paste could not be tied to one. It is what lets the store tell a repeat
/// of the same paste from a genuinely new one. `capturedAt` keeps its name because
/// it is the on-disk key; it is when the entry was recorded.
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

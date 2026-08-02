import Foundation

/// An invitation to paste what is currently on the pasteboard.
///
/// It deliberately carries no preview of the text: knowing the text would mean
/// reading it, and reading it is what raises the iOS paste alert. The user learns
/// only what kind of thing was copied until they choose to paste it.
public struct ClipboardOffer: Equatable, Sendable {
    public enum Kind: Equatable, Sendable {
        case text
        case link
    }

    public let kind: Kind
    public let changeCount: Int

    public init(kind: Kind, changeCount: Int) {
        self.kind = kind
        self.changeCount = changeCount
    }
}

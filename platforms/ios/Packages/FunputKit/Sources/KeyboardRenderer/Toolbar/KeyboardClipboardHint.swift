/// What the toolbar may say about the pasteboard.
///
/// Only a category, never the copied text: learning the text would mean reading
/// the pasteboard, which is what raises the iOS paste alert. The user finds out
/// what it actually says by pasting it.
public enum KeyboardClipboardHint: Equatable, Sendable {
    case text
    case link

    public var title: String {
        switch self {
        case .text: "Đã sao chép văn bản"
        case .link: "Đã sao chép một liên kết"
        }
    }
}

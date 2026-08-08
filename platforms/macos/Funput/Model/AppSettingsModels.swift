import Foundation

/// A text-expansion shortcut with a stable row identifier.
struct TextShortcut: Codable, Identifiable, Hashable {
    var id = UUID()
    var trigger: String
    var expansion: String
}

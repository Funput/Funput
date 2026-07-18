import Foundation

/// An app where Funput stays out of the way while it is the focused client.
struct ExcludedApp: Codable, Identifiable, Hashable {
    let id: String
    let name: String
}

/// A text-expansion shortcut with a stable row identifier.
struct TextShortcut: Codable, Identifiable, Hashable {
    var id = UUID()
    var trigger: String
    var expansion: String
}

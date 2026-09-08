import Foundation

/// A text-expansion shortcut with a stable row identifier.
struct TextShortcut: Codable, Identifiable, Hashable {
    var id = UUID()
    var trigger: String
    var expansion: String

    /// Draft rows stay editable but must not expand until both fields are filled.
    var isComplete: Bool {
        !trigger.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !expansion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

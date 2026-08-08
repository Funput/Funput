import Foundation

extension AppSettings {
    func addShortcut() {
        shortcuts.append(TextShortcut(trigger: "", expansion: ""))
    }

    func removeShortcut(_ id: UUID) {
        shortcuts.removeAll { $0.id == id }
    }
}

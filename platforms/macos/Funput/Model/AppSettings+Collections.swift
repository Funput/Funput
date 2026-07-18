import Foundation

extension AppSettings {
    func isExcluded(_ bundleId: String?) -> Bool {
        guard let bundleId, !bundleId.isEmpty else { return false }
        return excludedApps.contains { $0.id == bundleId }
    }

    func addExcludedApp(_ app: ExcludedApp) {
        guard !excludedApps.contains(where: { $0.id == app.id }) else { return }
        excludedApps.append(app)
    }

    func removeExcludedApp(_ id: String) {
        excludedApps.removeAll { $0.id == id }
    }

    func addShortcut() {
        shortcuts.append(TextShortcut(trigger: "", expansion: ""))
    }

    func removeShortcut(_ id: UUID) {
        shortcuts.removeAll { $0.id == id }
    }
}

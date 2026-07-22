import ServiceManagement

enum LoginItemController {
    static func setEnabled(_ enabled: Bool) {
        do {
            if enabled {
                try SMAppService.mainApp.register()
            } else {
                try SMAppService.mainApp.unregister()
            }
        } catch {
            // Unsigned development builds can't register; the preference remains
            // available and release builds retry on the next explicit change.
        }
    }
}

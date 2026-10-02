extension AppSettings {
    enum Keys {
        static let inputMethod = "inputMethod"
        static let toneStyle = "toneStyle"
        static let vietnameseEnabled = "vietnameseEnabled"
        static let smartEnglishRestore = "smartEnglishRestore"
        static let eagerRestore = "eagerRestore"
        static let spellCheckEnabled = "spellCheckEnabled"
        static let autoCapitalizeEnabled = "autoCapitalizeEnabled"
        static let retoneAfterBackspace = "retoneAfterBackspace"
        static let toggleShortcut = "toggleShortcut"
        static let flipShortcut = "flipShortcut"
        static let launchAtLogin = "launchAtLogin"
        static let showMenuBarIcon = "showMenuBarIcon"
        static let hasCompletedOnboarding = "hasCompletedOnboarding"
        static let shortcuts = "shortcuts"
        static let shortcutsInEnglish = "shortcutsInEnglish"
        static let shortcutsEnabled = "shortcutsEnabled"
        static let shortcutSmartCase = "shortcutSmartCase"
        static let extraOnsets = "extraOnsets"
    }

    /// Preferences that default to on. `UserDefaults.bool` reads a missing key as
    /// false, so these are registered before the first read.
    static let registeredDefaults: [String: Any] = [
        Keys.smartEnglishRestore: true,
        Keys.eagerRestore: true,
        Keys.showMenuBarIcon: true,
        Keys.vietnameseEnabled: true,
        Keys.retoneAfterBackspace: true,
        Keys.shortcutsEnabled: true,
        Keys.shortcutsInEnglish: true,
        Keys.shortcutSmartCase: true,
    ]
}

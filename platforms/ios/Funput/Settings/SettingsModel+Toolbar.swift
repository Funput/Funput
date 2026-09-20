import FunputShared

/// The toolbar switch is a view onto the two features the band carries rather than a
/// setting of its own: a stored flag could disagree with them — the band hidden while the
/// clipboard is on would leave the paste offer nowhere to go.
extension SettingsModel {
    /// On while anything the band carries is on, which is exactly when it is drawn.
    var showsToolbar: Bool { configuration.showsToolbar }

    /// Switches the band's features together. Turning it off costs the suggestions and
    /// the clipboard, which is why the row asks first.
    func setToolbarVisible(_ isVisible: Bool) {
        update { configuration in
            configuration.personalSuggestionsEnabled = isVisible
            configuration.clipboardEnabled = isVisible
        }
    }
}

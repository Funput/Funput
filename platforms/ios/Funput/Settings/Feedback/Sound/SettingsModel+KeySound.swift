import FunputShared

extension SettingsModel {
    var selectedKeySoundStyle: KeySoundStyle? {
        configuration.isKeySoundEnabled ? configuration.keySoundStyle : nil
    }

    /// Returns true only when the choice is already selected or was saved successfully.
    @discardableResult
    func selectKeySoundStyle(_ style: KeySoundStyle?) -> Bool {
        guard style == nil || hasFullAccess else { return false }
        return saveKeySoundStyle(style)
    }

    /// The volume is remembered on its own, so it needs no Full Access to save.
    @discardableResult
    func selectKeySoundVolume(_ volume: KeySoundVolume) -> Bool {
        if configuration.keySoundVolume == volume { return true }
        update(\.keySoundVolume, to: volume)
        return configuration.keySoundVolume == volume
    }

    func saveKeySoundStyle(_ style: KeySoundStyle?) -> Bool {
        if selectedKeySoundStyle == style { return true }
        update {
            $0.isKeySoundEnabled = style != nil
            if let style { $0.keySoundStyle = style }
        }
        return selectedKeySoundStyle == style
    }
}

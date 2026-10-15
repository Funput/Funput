import FunputShared

extension SettingsModel {
    func isOn(_ setting: QuickSetting) -> Bool {
        if setting == .numberRow, isNumberRowLocked { return true }
        return configuration[keyPath: setting.keyPath]
    }

    /// VNI types tones with digits, so its number row cannot be switched off.
    func isLocked(_ setting: QuickSetting) -> Bool {
        setting == .numberRow && isNumberRowLocked
    }

    func stateLabel(for setting: QuickSetting) -> String {
        if isLocked(setting) { return "Luôn bật" }
        return isOn(setting) ? "Bật" : "Tắt"
    }

    /// Flips a tile's switch.
    ///
    /// Returns the Full Access feature to ask for instead when the switch cannot work
    /// without it — the same rule as ``fullAccessBinding(_:requestAccess:)``.
    @discardableResult
    func toggle(_ setting: QuickSetting) -> FullAccessFeature? {
        guard !isLocked(setting) else { return nil }
        let enabling = !isOn(setting)
        if enabling, !hasFullAccess, let feature = setting.fullAccessFeature { return feature }
        update(setting.keyPath, to: enabling)
        return nil
    }
}

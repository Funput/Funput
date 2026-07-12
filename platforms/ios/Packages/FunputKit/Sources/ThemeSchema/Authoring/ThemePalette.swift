/// The adaptive color roles an authored theme defines.
///
/// Roles are semantic, not positional: the renderer decides which element uses
/// which role, so a theme can never move or resize a key by recoloring it.
public struct ThemePalette: Codable, Hashable, Sendable {
    public var backgroundStart: AdaptiveThemeColor
    public var backgroundEnd: AdaptiveThemeColor
    public var characterKey: AdaptiveThemeColor
    public var specialKey: AdaptiveThemeColor
    public var border: AdaptiveThemeColor
    public var label: AdaptiveThemeColor
    public var secondaryLabel: AdaptiveThemeColor
    public var accent: AdaptiveThemeColor

    public init(
        backgroundStart: AdaptiveThemeColor,
        backgroundEnd: AdaptiveThemeColor,
        characterKey: AdaptiveThemeColor,
        specialKey: AdaptiveThemeColor,
        border: AdaptiveThemeColor,
        label: AdaptiveThemeColor,
        secondaryLabel: AdaptiveThemeColor,
        accent: AdaptiveThemeColor
    ) {
        self.backgroundStart = backgroundStart
        self.backgroundEnd = backgroundEnd
        self.characterKey = characterKey
        self.specialKey = specialKey
        self.border = border
        self.label = label
        self.secondaryLabel = secondaryLabel
        self.accent = accent
    }
}

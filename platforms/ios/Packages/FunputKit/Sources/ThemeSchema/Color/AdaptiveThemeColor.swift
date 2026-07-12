/// A color that resolves to a different value in light and dark appearances.
///
/// The renderer selects `light` or `dark` at draw time from the active trait
/// collection; the schema only stores the pair.
public struct AdaptiveThemeColor: Hashable, Sendable, Codable {
    public let light: ThemeRGBA
    public let dark: ThemeRGBA

    public init(light: ThemeRGBA, dark: ThemeRGBA) {
        self.light = light
        self.dark = dark
    }
}

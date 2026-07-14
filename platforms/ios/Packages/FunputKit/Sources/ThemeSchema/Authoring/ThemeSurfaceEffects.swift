/// Optional overrides layered around native Liquid Glass key surfaces.
///
/// Disabled overrides preserve the system-rendered Glass appearance for
/// bundled and legacy themes. Non-Glass materials always use authored metrics.
public struct ThemeSurfaceEffects: Codable, Hashable, Sendable {
    public var glassBorderOverrideEnabled: Bool
    public var glassShadowOverrideEnabled: Bool

    public init(
        glassBorderOverrideEnabled: Bool = false,
        glassShadowOverrideEnabled: Bool = false
    ) {
        self.glassBorderOverrideEnabled = glassBorderOverrideEnabled
        self.glassShadowOverrideEnabled = glassShadowOverrideEnabled
    }

    public static let `default` = ThemeSurfaceEffects()
}

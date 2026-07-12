import ThemeSchema

/// The read-only catalog of themes shipped inside the app binary.
///
/// Bundled themes are always available offline and act as the guaranteed
/// fallback when a selected or downloaded theme is missing or fails to load.
public enum BundledThemes {
    /// Every theme shipped with the app, in display order.
    public static let all: [KeyboardTheme] = [.funputGlass, .classicLight, .midnight]

    /// The theme used when no valid selection exists.
    public static let `default`: KeyboardTheme = .funputGlass

    /// Looks up a bundled theme by identifier.
    public static func theme(id: String) -> KeyboardTheme? {
        all.first { $0.id == id }
    }
}

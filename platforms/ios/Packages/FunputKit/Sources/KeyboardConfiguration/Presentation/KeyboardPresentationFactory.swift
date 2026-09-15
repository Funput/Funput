#if canImport(UIKit)
import FunputShared
import KeyboardLayout
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

/// Bridges shared ``FunputConfiguration`` to a renderer ``KeyboardPresentation``,
/// resolving the selected theme through ``ThemeRuntime``.
///
/// The keyboard extension uses ``resolvedTheme(for:)`` to fold configuration
/// into its live, state-driven presentation; the in-app preview uses ``make``
/// to build a full presentation. Both share one theme-resolution path.
@MainActor
public enum KeyboardPresentationFactory {
    /// Builds a complete presentation for surfaces without live input state,
    /// such as the in-app theme preview.
    public static func make(
        from configuration: FunputConfiguration,
        layout: KeyboardLayout = .funputQWERTY,
        catalog: ThemeCatalog = ThemeCatalog()
    ) -> KeyboardPresentation {
        let theme = resolvedTheme(for: configuration, catalog: catalog)
        return KeyboardPresentation(
            layout: layout,
            sizing: sizing(for: configuration, theme: theme),
            theme: theme,
            blendsSystemEdge: selectedTheme(for: configuration, catalog: catalog).id
                != BundledThemes.default.id,
            language: configuration.language,
            isHapticFeedbackEnabled: configuration.isHapticFeedbackEnabled,
            isKeySoundEnabled: configuration.isKeySoundEnabled,
            showsKeyPreviews: configuration.showsKeyPreviews,
            areSmartGesturesEnabled: configuration.smartGesturesEnabled,
            pinsAppearance: configuration.keyboardAppearance != .system
        )
    }

    /// Resolves the configuration's selected bundled theme, honoring Reduce
    /// Transparency. Falls back to the bundled default for an unknown id.
    public static func resolvedTheme(
        for configuration: FunputConfiguration,
        catalog: ThemeCatalog = ThemeCatalog()
    ) -> ResolvedTheme {
        let authored = selectedTheme(for: configuration, catalog: catalog)
        let context = ThemeResolveContext(
            reduceTransparency: UIAccessibility.isReduceTransparencyEnabled
        )
        return ThemeRuntime.resolve(authored, context: context)
    }

    /// System sizing replaces the theme's gaps and the height setting with Apple's
    /// metrics; Funput sizing takes both from the theme and the user.
    public static func sizing(
        for configuration: FunputConfiguration,
        theme: ResolvedTheme
    ) -> KeyboardSizingProfile {
        guard configuration.keySizing == .funput else { return .system }
        var sizing = KeyboardSizingProfile.default
        sizing.heightScale = CGFloat(configuration.heightScale)
        sizing.horizontalPadding = CGFloat(theme.horizontalPadding)
        sizing.horizontalGap = CGFloat(theme.horizontalGap)
        sizing.verticalGap = CGFloat(theme.verticalGap)
        return sizing
    }

    private static func selectedTheme(
        for configuration: FunputConfiguration,
        catalog: ThemeCatalog
    ) -> KeyboardTheme {
        catalog.theme(id: configuration.selectedThemeID) ?? BundledThemes.default
    }
}
#endif

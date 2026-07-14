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
        var sizing = KeyboardSizingProfile.default
        sizing.heightScale = CGFloat(configuration.heightScale)
        let theme = resolvedTheme(for: configuration, catalog: catalog)
        sizing.horizontalPadding = CGFloat(theme.horizontalPadding)
        sizing.horizontalGap = CGFloat(theme.horizontalGap)
        sizing.verticalGap = CGFloat(theme.verticalGap)
        return KeyboardPresentation(
            layout: layout,
            sizing: sizing,
            theme: theme,
            language: configuration.language,
            isHapticFeedbackEnabled: configuration.isHapticFeedbackEnabled,
            isKeySoundEnabled: configuration.isKeySoundEnabled,
            showsKeyPreviews: configuration.showsKeyPreviews
        )
    }

    /// Resolves the configuration's selected bundled theme, honoring Reduce
    /// Transparency. Falls back to the bundled default for an unknown id.
    public static func resolvedTheme(
        for configuration: FunputConfiguration,
        catalog: ThemeCatalog = ThemeCatalog()
    ) -> ResolvedTheme {
        let authored = catalog.theme(id: configuration.selectedThemeID) ?? BundledThemes.default
        let context = ThemeResolveContext(
            reduceTransparency: UIAccessibility.isReduceTransparencyEnabled
        )
        return ThemeRuntime.resolve(authored, context: context)
    }
}
#endif

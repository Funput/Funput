import ThemeSchema

/// Resolves declarative ``KeyboardTheme`` values into render-ready
/// ``ResolvedTheme`` values.
///
/// Resolution is pure and synchronous: metrics are clamped to safe ranges and
/// the material is downgraded when transparency is reduced. Light/dark color
/// selection stays at render time (the renderer picks from the trait
/// collection), so this step never needs UIKit.
public enum ThemeRuntime {
    public static func resolve(
        _ theme: KeyboardTheme,
        context: ThemeResolveContext = .standard
    ) -> ResolvedTheme {
        let palette = theme.palette
        let metrics = theme.metrics
        return ResolvedTheme(
            material: resolvedMaterial(theme.material, context: context),
            backgroundStart: palette.backgroundStart,
            backgroundEnd: palette.backgroundEnd,
            characterKey: palette.characterKey,
            specialKey: palette.specialKey,
            border: palette.border,
            label: palette.label,
            secondaryLabel: palette.secondaryLabel,
            accent: palette.accent,
            keyOpacity: MetricClamp.unit(metrics.keyOpacity),
            specialKeyOpacity: MetricClamp.unit(metrics.specialKeyOpacity),
            cornerRadius: MetricClamp.cornerRadius(metrics.cornerRadius),
            borderWidth: MetricClamp.borderWidth(metrics.borderWidth),
            shadowOpacity: MetricClamp.unit(metrics.shadowOpacity),
            shadowRadius: MetricClamp.shadowRadius(metrics.shadowRadius),
            pressedScale: MetricClamp.pressedScale(metrics.pressedScale),
            pressedOpacityMultiplier: MetricClamp.pressedOpacityMultiplier(metrics.pressedOpacityMultiplier),
            fontScale: MetricClamp.fontScale(metrics.fontScale)
        )
    }

    /// Reduce Transparency collapses translucent and glass surfaces to a solid
    /// fill. The renderer enforces the same rule live; doing it here keeps the
    /// resolved value correct for non-renderer consumers and snapshot tests.
    private static func resolvedMaterial(
        _ material: KeyboardMaterial,
        context: ThemeResolveContext
    ) -> KeyboardMaterial {
        context.reduceTransparency ? .solid : material
    }
}

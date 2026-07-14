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
        let geometry = theme.geometry
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
            keyOpacity: resolvedOpacity(metrics.keyOpacity, context: context),
            specialKeyOpacity: resolvedOpacity(metrics.specialKeyOpacity, context: context),
            cornerRadius: MetricClamp.cornerRadius(metrics.cornerRadius),
            borderWidth: MetricClamp.borderWidth(metrics.borderWidth),
            shadowOpacity: MetricClamp.unit(metrics.shadowOpacity),
            shadowRadius: MetricClamp.shadowRadius(metrics.shadowRadius),
            pressedScale: MetricClamp.pressedScale(metrics.pressedScale),
            pressedOpacityMultiplier: MetricClamp.pressedOpacityMultiplier(metrics.pressedOpacityMultiplier),
            fontScale: MetricClamp.fontScale(metrics.fontScale),
            keycapHeightScale: MetricClamp.keycapHeight(geometry.keycapHeightScale),
            horizontalPadding: MetricClamp.horizontalPadding(geometry.horizontalPadding),
            horizontalGap: MetricClamp.horizontalGap(geometry.horizontalGap),
            verticalGap: MetricClamp.verticalGap(geometry.verticalGap),
            colorEffects: theme.colorEffects,
            surfaceEffects: theme.surfaceEffects,
            gradientDirection: theme.gradientDirection,
            backgroundEffects: resolvedBackground(theme.backgroundEffects)
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

    private static func resolvedOpacity(
        _ opacity: Double,
        context: ThemeResolveContext
    ) -> Double {
        context.reduceTransparency ? 1 : MetricClamp.unit(opacity)
    }

    private static func resolvedBackground(
        _ effects: ThemeBackgroundEffects
    ) -> ThemeBackgroundEffects {
        guard var image = effects.image else { return effects }
        image.focalX = MetricClamp.imageFocalPoint(image.focalX)
        image.focalY = MetricClamp.imageFocalPoint(image.focalY)
        image.zoom = MetricClamp.imageZoom(image.zoom)
        image.blurRadius = MetricClamp.imageBlur(image.blurRadius)
        return ThemeBackgroundEffects(
            mode: effects.mode,
            image: image,
            overlay: clamped(effects.overlay)
        )
    }

    private static func clamped(_ color: AdaptiveThemeColor) -> AdaptiveThemeColor {
        AdaptiveThemeColor(light: clamped(color.light), dark: clamped(color.dark))
    }

    private static func clamped(_ color: ThemeRGBA) -> ThemeRGBA {
        ThemeRGBA(
            red: MetricClamp.unit(color.red),
            green: MetricClamp.unit(color.green),
            blue: MetricClamp.unit(color.blue),
            alpha: MetricClamp.unit(color.alpha)
        )
    }
}

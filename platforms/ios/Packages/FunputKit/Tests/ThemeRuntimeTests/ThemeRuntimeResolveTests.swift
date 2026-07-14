import Testing
import ThemeSchema
import ThemeRuntime

struct ThemeRuntimeResolveTests {
    @Test("Bundled Funput Glass resolves to the renderer default")
    func funputGlassParity() {
        #expect(ThemeRuntime.resolve(.funputGlass) == ResolvedTheme.funputGlass)
    }

    @Test("Bundled themes share the Classic key corner radius")
    func bundledKeyCornerRadius() {
        let classicRadius = ThemeRuntime.resolve(.classicLight).cornerRadius

        #expect(ThemeRuntime.resolve(.funputGlass).cornerRadius == classicRadius)
        #expect(ThemeRuntime.resolve(.midnight).cornerRadius == classicRadius)
    }

    @Test("Standard context preserves the authored material")
    func standardKeepsMaterial() {
        #expect(ThemeRuntime.resolve(.funputGlass).material == .glass)
        #expect(ThemeRuntime.resolve(.classicLight).material == .translucent)
    }

    @Test("Reduce Transparency downgrades every material to solid")
    func reduceTransparencyForcesSolid() {
        let context = ThemeResolveContext(reduceTransparency: true)
        #expect(ThemeRuntime.resolve(.funputGlass, context: context).material == .solid)
        #expect(ThemeRuntime.resolve(.classicLight, context: context).material == .solid)
    }

    @Test("Out-of-range metrics are clamped into safe ranges")
    func clampsMetrics() {
        let wild = KeyboardTheme(
            id: "test.wild",
            metadata: ThemeMetadata(name: "Wild", author: "Test"),
            material: .solid,
            palette: BundledThemes.default.palette,
            metrics: ThemeMetrics(
                keyOpacity: 4,
                specialKeyOpacity: -1,
                cornerRadius: 999,
                borderWidth: 50,
                shadowOpacity: 9,
                shadowRadius: 500,
                pressedScale: 0.1,
                pressedOpacityMultiplier: 8,
                fontScale: 12
            )
        )
        let resolved = ThemeRuntime.resolve(wild)
        #expect(resolved.keyOpacity == 1)
        #expect(resolved.specialKeyOpacity == 0)
        #expect(resolved.cornerRadius == 20)
        #expect(resolved.borderWidth == 4)
        #expect(resolved.shadowOpacity == 1)
        #expect(resolved.shadowRadius == 24)
        #expect(resolved.pressedScale == 0.8)
        #expect(resolved.pressedOpacityMultiplier == 1.5)
        #expect(resolved.fontScale == 1.4)
    }

    @Test("Geometry is clamped to MVP safe ranges")
    func clampsGeometry() {
        var wild = BundledThemes.default
        wild.geometry = ThemeGeometry(
            keycapHeightScale: 0,
            horizontalPadding: 99,
            horizontalGap: -5,
            verticalGap: 50
        )
        let resolved = ThemeRuntime.resolve(wild)

        #expect(resolved.keycapHeightScale == 0.82)
        #expect(resolved.horizontalPadding == 16)
        #expect(resolved.horizontalGap == 2)
        #expect(resolved.verticalGap == 12)
    }

    @Test("Color effects pass through runtime resolution")
    func resolvesColorEffects() {
        var theme = BundledThemes.default
        theme.colorEffects.glassBackgroundTintEnabled = true
        theme.colorEffects.glassKeyTintEnabled = true
        theme.colorEffects.pressedOverlayEnabled = true

        #expect(ThemeRuntime.resolve(theme).colorEffects == theme.colorEffects)
    }
}

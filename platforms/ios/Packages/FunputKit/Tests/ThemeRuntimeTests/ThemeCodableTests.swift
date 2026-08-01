import Foundation
import Testing
import ThemeSchema
import ThemeRuntime

struct ThemeCodableTests {
    @Test("KeyboardTheme survives a JSON round-trip", arguments: BundledThemes.all)
    func roundTrip(_ theme: KeyboardTheme) throws {
        let data = try JSONEncoder().encode(theme)
        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(decoded == theme)
    }

    @Test("Bundled theme identifiers are unique")
    func uniqueIdentifiers() {
        let ids = BundledThemes.all.map(\.id)
        #expect(ids == [
            KeyboardTheme.funputGlass.id,
            KeyboardTheme.classicLight.id,
            KeyboardTheme.midnight.id,
            KeyboardTheme.lotusSilk.id,
            KeyboardTheme.jadeCurrent.id,
        ])
        #expect(Set(ids).count == ids.count)
    }

    @Test("Lookup returns the matching bundled theme, or nil")
    func lookup() {
        #expect(BundledThemes.theme(id: BundledThemes.default.id) == .funputGlass)
        #expect(BundledThemes.theme(id: KeyboardTheme.lotusSilk.id) == .lotusSilk)
        #expect(BundledThemes.theme(id: KeyboardTheme.jadeCurrent.id) == .jadeCurrent)
        #expect(BundledThemes.theme(id: "does.not.exist") == nil)
    }

    @Test("Schema v1 without geometry receives current defaults")
    func migratesV1Geometry() throws {
        let encoded = try JSONEncoder().encode(BundledThemes.default)
        var object = try #require(JSONSerialization.jsonObject(with: encoded) as? [String: Any])
        object["schemaVersion"] = 1
        object.removeValue(forKey: "geometry")
        let data = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(decoded.schemaVersion == 1)
        #expect(decoded.geometry == .default)
    }

    @Test("Custom theme survives a JSON round-trip")
    func customRoundTrip() throws {
        var custom = CustomKeyboardTheme(
            baseTheme: .midnight,
            uuid: UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")!
        )
        custom.theme.geometry = ThemeGeometry(
            keycapHeightScale: 0.88,
            horizontalPadding: 12,
            horizontalGap: 4,
            verticalGap: 9
        )
        custom.theme.colorEffects.glassKeyTintEnabled = true
        custom.theme.colorEffects.pressedOverlayEnabled = true
        custom.theme.surfaceEffects.glassBorderOverrideEnabled = true
        custom.theme.surfaceEffects.glassShadowOverrideEnabled = true
        custom.theme.gradientDirection = .horizontal
        custom.theme.backgroundEffects = ThemeBackgroundEffects(
            mode: .image,
            image: ThemeBackgroundImage(
                assetID: "wallpaper",
                focalX: 0.2,
                focalY: 0.8,
                zoom: 2.5,
                blurRadius: 12
            ),
            overlay: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.15),
                dark: ThemeRGBA(hex: 0x000000, alpha: 0.25)
            )
        )

        let data = try JSONEncoder().encode(custom)
        #expect(try JSONDecoder().decode(CustomKeyboardTheme.self, from: data) == custom)
    }

    @Test("Schema v2 receives disabled color effects seeded from accent")
    func migratesV2ColorEffects() throws {
        let encoded = try JSONEncoder().encode(BundledThemes.default)
        var object = try #require(JSONSerialization.jsonObject(with: encoded) as? [String: Any])
        object["schemaVersion"] = 2
        object.removeValue(forKey: "colorEffects")
        let data = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(!decoded.colorEffects.glassBackgroundTintEnabled)
        #expect(!decoded.colorEffects.glassKeyTintEnabled)
        #expect(!decoded.colorEffects.pressedOverlayEnabled)
        #expect(decoded.colorEffects.pressedOverlay == decoded.palette.accent)
    }

    @Test("Schema v3 receives disabled Glass surface overrides")
    func migratesV3SurfaceEffects() throws {
        let encoded = try JSONEncoder().encode(BundledThemes.default)
        var object = try #require(JSONSerialization.jsonObject(with: encoded) as? [String: Any])
        object["schemaVersion"] = 3
        object.removeValue(forKey: "surfaceEffects")
        let data = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(!decoded.surfaceEffects.glassBorderOverrideEnabled)
        #expect(!decoded.surfaceEffects.glassShadowOverrideEnabled)
    }

    @Test("Schema v4 receives the legacy diagonal gradient direction")
    func migratesV4GradientDirection() throws {
        let encoded = try JSONEncoder().encode(BundledThemes.default)
        var object = try #require(JSONSerialization.jsonObject(with: encoded) as? [String: Any])
        object["schemaVersion"] = 4
        object.removeValue(forKey: "gradientDirection")
        let data = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(decoded.gradientDirection == .diagonalRight)
    }

    @Test("Schema v5 receives the gradient background default")
    func migratesV5Background() throws {
        let encoded = try JSONEncoder().encode(BundledThemes.default)
        var object = try #require(JSONSerialization.jsonObject(with: encoded) as? [String: Any])
        object["schemaVersion"] = 5
        object.removeValue(forKey: "backgroundEffects")
        let data = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(decoded.backgroundEffects == .default)
    }
}

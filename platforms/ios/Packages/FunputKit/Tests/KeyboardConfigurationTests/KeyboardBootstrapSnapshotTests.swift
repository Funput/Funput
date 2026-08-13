import Foundation
import FunputShared
import KeyboardConfiguration
import Testing
import ThemeRuntime
import ThemeSchema

struct KeyboardBootstrapSnapshotTests {
    @Test("Snapshot round-trips bundled and custom image themes")
    func roundTrip() throws {
        var configuration = FunputConfiguration.default
        var custom = CustomKeyboardTheme(baseTheme: .midnight)
        custom.theme.backgroundEffects.mode = .image
        custom.theme.backgroundEffects.image = ThemeBackgroundImage(
            assetID: "image-asset"
        )
        configuration.selectedThemeID = custom.id

        let snapshot = KeyboardBootstrapSnapshot.make(
            configuration: configuration,
            customThemes: [custom]
        )
        let data = try JSONEncoder().encode(snapshot)
        let decoded = try JSONDecoder().decode(
            KeyboardBootstrapSnapshot.self,
            from: data
        )

        #expect(decoded == snapshot)
        #expect(decoded.selectedTheme == custom.theme)
        #expect(decoded.selectedTheme.backgroundEffects.image?.assetID == "image-asset")
    }

    @Test("Snapshot normalizes bounds and missing themes")
    func normalization() {
        var configuration = FunputConfiguration.default
        configuration.heightScale = 5
        configuration.schemaVersion = 1
        configuration.selectedThemeID = "missing"

        let snapshot = KeyboardBootstrapSnapshot.make(
            configuration: configuration,
            customThemes: []
        )

        #expect(snapshot.configuration.heightScale == 1.2)
        #expect(snapshot.configuration.schemaVersion == FunputConfiguration.currentSchemaVersion)
        #expect(snapshot.configuration.selectedThemeID == BundledThemes.default.id)
        #expect(snapshot.selectedTheme == BundledThemes.default)
    }

    @Test("Old, future, and inconsistent snapshots fail as a whole")
    func invalidSchema() throws {
        let snapshot = KeyboardBootstrapSnapshot.make(
            configuration: .default,
            customThemes: []
        )
        let encoded = try JSONEncoder().encode(snapshot)

        for version in [0, KeyboardBootstrapSnapshot.currentSchemaVersion + 1] {
            let data = try replacing(encoded, key: "schemaVersion", value: version)
            #expect(throws: (any Error).self) {
                try JSONDecoder().decode(KeyboardBootstrapSnapshot.self, from: data)
            }
        }
        let inconsistent = try replacing(
            encoded,
            key: "configuration.selectedThemeID",
            value: "missing"
        )
        #expect(throws: (any Error).self) {
            try JSONDecoder().decode(KeyboardBootstrapSnapshot.self, from: inconsistent)
        }
        let unnormalized = try replacing(
            encoded,
            key: "configuration.heightScale",
            value: 5
        )
        #expect(throws: (any Error).self) {
            try JSONDecoder().decode(KeyboardBootstrapSnapshot.self, from: unnormalized)
        }
    }

    private func replacing(_ data: Data, key: String, value: Any) throws -> Data {
        var json = try #require(JSONSerialization.jsonObject(with: data) as? [String: Any])
        if key.hasPrefix("configuration.") {
            var configuration = try #require(json["configuration"] as? [String: Any])
            configuration[String(key.dropFirst("configuration.".count))] = value
            json["configuration"] = configuration
        } else {
            json[key] = value
        }
        return try JSONSerialization.data(withJSONObject: json)
    }
}

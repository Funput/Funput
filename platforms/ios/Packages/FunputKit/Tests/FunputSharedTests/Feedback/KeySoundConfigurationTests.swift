import Foundation
import FunputShared
import Testing

struct KeySoundConfigurationTests {
    @Test("Defaults keep the system click at the middle volume")
    func defaults() {
        let config = FunputConfiguration.default
        #expect(!config.isKeySoundEnabled)
        #expect(config.keySoundStyle == .system)
        #expect(config.keySoundVolume == .medium)
    }

    @Test("Schema 17 keeps the switch and plays the system click", arguments: [false, true])
    func legacyConfiguration(enabled: Bool) throws {
        let data = Data("{\"isKeySoundEnabled\":\(enabled),\"schemaVersion\":17}".utf8)
        let config = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(config.isKeySoundEnabled == enabled)
        #expect(config.keySoundStyle == .system)
        #expect(config.keySoundVolume == .medium)
        #expect(config.schemaVersion == FunputConfiguration.currentSchemaVersion)
    }

    @Test("Style and volume survive persistence while sound is off", arguments: KeySoundStyle.allCases)
    func roundTrip(style: KeySoundStyle) throws {
        var config = FunputConfiguration.default
        config.keySoundStyle = style
        config.keySoundVolume = .loud
        let data = try JSONEncoder().encode(config)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded == config)
        #expect(!decoded.isKeySoundEnabled)
    }

    @Test("Unknown values fall back without discarding other settings")
    func unknownValues() throws {
        let json = """
        {"keySoundStyle":"vinyl","keySoundVolume":"deafening","isKeySoundEnabled":true,\
        "showsNumberRow":true,"schemaVersion":18}
        """
        let config = try JSONDecoder().decode(FunputConfiguration.self, from: Data(json.utf8))
        #expect(config.keySoundStyle == .system)
        #expect(config.keySoundVolume == .medium)
        #expect(config.isKeySoundEnabled)
        #expect(config.showsNumberRow)
    }
}

import Foundation
import FunputShared
import Testing

struct HapticFeedbackConfigurationTests {
    @Test("Schema 15 keeps the switch and supplies the light strength", arguments: [false, true])
    func legacyConfiguration(enabled: Bool) throws {
        let data = Data("{\"isHapticFeedbackEnabled\":\(enabled),\"schemaVersion\":15}".utf8)
        let config = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(config.isHapticFeedbackEnabled == enabled)
        #expect(config.hapticFeedbackStrength == .light)
        #expect(config.schemaVersion == FunputConfiguration.currentSchemaVersion)
    }

    @Test("Strength survives persistence while feedback is off", arguments: HapticFeedbackStrength.allCases)
    func roundTrip(strength: HapticFeedbackStrength) throws {
        var config = FunputConfiguration.default
        config.hapticFeedbackStrength = strength
        let data = try JSONEncoder().encode(config)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded == config)
        #expect(!decoded.isHapticFeedbackEnabled)
    }

    @Test("An unknown strength falls back without discarding other settings")
    func unknownStrength() throws {
        let json = "{\"hapticFeedbackStrength\":\"extraStrong\",\"isHapticFeedbackEnabled\":true,\"schemaVersion\":16}"
        let config = try JSONDecoder().decode(FunputConfiguration.self, from: Data(json.utf8))
        #expect(config.hapticFeedbackStrength == .light)
        #expect(config.isHapticFeedbackEnabled)
    }
}

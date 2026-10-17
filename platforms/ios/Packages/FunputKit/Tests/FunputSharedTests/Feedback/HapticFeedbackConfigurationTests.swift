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
        #expect(config.schemaVersion == 16)
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
}

import Foundation
import FunputShared
import KeyboardLayout
import Testing

struct AdvancedTelexConfigurationTests {
    @Test("Advanced Telex uses its stable persistence identifier")
    func stableIdentifier() {
        #expect(KeyboardInputMethod.allCases == [.telex, .telexAdvanced, .vni])
        #expect(KeyboardInputMethod.telexAdvanced.rawValue == "telex_advanced")
    }

    @Test("Advanced Telex survives configuration round-trip")
    func roundTrip() throws {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .telexAdvanced

        let data = try JSONEncoder().encode(configuration)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)

        #expect(decoded.inputMethod == .telexAdvanced)
        #expect(decoded.schemaVersion == 11)
    }

    @Test("Legacy schema preserves its input method while migrating")
    func legacyMigration() throws {
        let data = Data(
            #"{"inputMethod":"telex","schemaVersion":6}"#.utf8
        )
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)

        #expect(decoded.inputMethod == .telex)
        #expect(decoded.schemaVersion == 11)
    }
}

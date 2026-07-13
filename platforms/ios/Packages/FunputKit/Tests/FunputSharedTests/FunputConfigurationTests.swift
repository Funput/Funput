import Foundation
import Testing
import KeyboardLayout
import FunputShared

struct FunputConfigurationTests {
    @Test("Default mirrors the engine Session defaults (unchanged typing behavior)")
    func defaults() {
        let config = FunputConfiguration.default
        #expect(config.inputMethod == .vni) // iOS overrides the engine's Telex default
        #expect(config.language == .vietnamese)
        #expect(config.toneStyle == .traditional) // funput-engine Session::new()
        #expect(config.spellCheck == false)
        #expect(config.smartRestore == true)
        #expect(config.eagerRestore == true)
        #expect(config.autoCapitalize == false)
        #expect(config.selectedThemeID == FunputConfiguration.defaultThemeID)
        #expect(!config.isHapticFeedbackEnabled)
        #expect(!config.isKeySoundEnabled)
    }

    @Test("Configuration survives a JSON round-trip")
    func roundTrip() throws {
        var config = FunputConfiguration.default
        config.inputMethod = .telex
        config.toneStyle = .traditional
        config.spellCheck = false
        config.heightScale = 1.1
        let data = try JSONEncoder().encode(config)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded == config)
    }

    @Test("Missing keys decode to their defaults")
    func partialDecodeFillsDefaults() throws {
        let json = Data(#"{"inputMethod":"telex"}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: json)
        #expect(decoded.inputMethod == .telex)
        #expect(decoded.language == FunputConfiguration.default.language)
        #expect(decoded.selectedThemeID == FunputConfiguration.defaultThemeID)
        #expect(decoded.showsKeyPreviews == FunputConfiguration.default.showsKeyPreviews)
    }

    @Test("Legacy feedback settings migrate to safe defaults")
    func migratesHapticDefault() throws {
        let data = Data(#"{"isHapticFeedbackEnabled":true,"schemaVersion":1}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(!decoded.isHapticFeedbackEnabled)
        #expect(!decoded.isKeySoundEnabled)
        #expect(decoded.schemaVersion == 3)
    }
}

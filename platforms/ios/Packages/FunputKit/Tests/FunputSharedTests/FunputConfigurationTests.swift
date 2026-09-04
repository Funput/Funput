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
        #expect(config.toneStyle == .modern)
        #expect(config.spellCheck == false)
        #expect(config.smartRestore == true)
        #expect(config.eagerRestore == true)
        #expect(config.autoCapitalize == true) // iOS overrides the engine's off default
        #expect(config.selectedThemeID == FunputConfiguration.defaultThemeID)
        #expect(!config.isHapticFeedbackEnabled)
        #expect(!config.isKeySoundEnabled)
        #expect(!config.showsNumberRow)
        #expect(config.heightScale == 1.1)
        #expect(config.personalSuggestionsEnabled)
        #expect(config.personalSuggestionResetToken == nil)
        #expect(config.clipboardEnabled)
        #expect(config.clipboardExpiry == .hour)
        #expect(config.layoutPreset == .funput)
        #expect(config.keyboardAppearance == .system) // follow the host app, as before v12
        #expect(config.schemaVersion == 12)
    }

    @Test("Configuration survives a JSON round-trip")
    func roundTrip() throws {
        var config = FunputConfiguration.default
        config.inputMethod = .telex
        config.toneStyle = .traditional
        config.spellCheck = false
        config.showsNumberRow = true
        config.heightScale = 1.1
        config.layoutPreset = .system
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
        #expect(!decoded.showsNumberRow)
        #expect(decoded.personalSuggestionsEnabled)
        #expect(decoded.layoutPreset == .funput)
        #expect(decoded.toneStyle == .traditional)
    }

    @Test("Stored tone styles remain unchanged")
    func storedToneStylesWinOverDefaults() throws {
        for style in ToneStyleOption.allCases {
            let data = Data(#"{"toneStyle":"\#(style.rawValue)"}"#.utf8)
            let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
            #expect(decoded.toneStyle == style)
        }
    }

    @Test("Legacy feedback settings migrate to safe defaults")
    func migratesHapticDefault() throws {
        let data = Data(#"{"isHapticFeedbackEnabled":true,"schemaVersion":1}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(!decoded.isHapticFeedbackEnabled)
        #expect(!decoded.isKeySoundEnabled)
        #expect(!decoded.showsNumberRow)
        #expect(decoded.schemaVersion == 12)
    }

    @Test("Schema 3 migrates to the compact Telex default")
    func migratesNumberRowDefault() throws {
        let data = Data(#"{"showsNumberRow":true,"schemaVersion":3}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(!decoded.showsNumberRow)
        #expect(decoded.schemaVersion == 12)
    }

    /// v8 dropped `showsGlobeKey` entirely. A stored payload still carrying it must
    /// decode without complaint rather than throwing on the unknown key.
    @Test("A payload from before the Globe key was dropped still decodes")
    func migratesPastGlobeKey() throws {
        let data = Data(#"{"showsGlobeKey":true,"showsNumberRow":true,"schemaVersion":4}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded.showsNumberRow)
        #expect(decoded.schemaVersion == 12)
    }

    /// v10 added `layoutPreset`. Unlike the `< 4` rung it must not touch stored values:
    /// upgrading users keep the layout they have been typing on, and their other
    /// settings survive intact.
    @Test("Schema 9 payloads keep the Funput layout preset")
    func migratesLayoutPresetDefault() throws {
        let data = Data(#"{"inputMethod":"telex","showsNumberRow":true,"schemaVersion":9}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded.layoutPreset == .funput)
        #expect(decoded.inputMethod == .telex)
        #expect(decoded.showsNumberRow)
        #expect(decoded.schemaVersion == 12)
    }

    /// v12 added `keyboardAppearance`. `.system` reproduces how every earlier build
    /// rendered, so an upgrading payload must land there with nothing else disturbed.
    @Test("Schema 11 payloads keep following the host app's appearance")
    func migratesKeyboardAppearanceDefault() throws {
        let data = Data(#"{"inputMethod":"telex","layoutPreset":"system","schemaVersion":11}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded.keyboardAppearance == .system)
        #expect(decoded.inputMethod == .telex)
        #expect(decoded.layoutPreset == .system)
        #expect(decoded.schemaVersion == 12)
    }
}

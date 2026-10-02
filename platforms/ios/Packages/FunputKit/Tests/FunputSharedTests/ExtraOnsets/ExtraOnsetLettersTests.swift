import Foundation
import FunputShared
import KeyboardLayout
import Testing

struct ExtraOnsetLettersTests {
    @Test("Selected letters serialize in display order")
    func configValue() {
        #expect(ExtraOnsetLetters([.j, .z]).configValue == "zj")
        #expect(ExtraOnsetLetters.all.configValue == "zfwj")
        #expect(ExtraOnsetLetters().configValue.isEmpty)
        #expect(ExtraOnsetLetters.letters.map(\.symbol) == ["z", "f", "w", "j"])
        #expect(Set(ExtraOnsetLetters.letters.map(\.id)).count == 4)
    }

    @Test("Parsing ignores case, duplicates and unsupported letters")
    func parsing() {
        #expect(ExtraOnsetLetters(configValue: "JzZ?x") == [.z, .j])
        #expect(ExtraOnsetLetters(configValue: "JWFZ") == .all)
        #expect(ExtraOnsetLetters(configValue: "abc") == [])
    }

    @Test("Codable stores one canonical string")
    func codable() throws {
        let letters: ExtraOnsetLetters = [.j, .z]
        let data = try JSONEncoder().encode(letters)
        #expect(String(decoding: data, as: UTF8.self) == #""zj""#)
        #expect(try JSONDecoder().decode(ExtraOnsetLetters.self, from: data) == letters)
        let unordered = Data(#""JzZ?x""#.utf8)
        #expect(try JSONDecoder().decode(ExtraOnsetLetters.self, from: unordered) == letters)
    }

    @Test("Fresh and legacy configurations leave extra onsets off")
    func defaults() throws {
        #expect(FunputConfiguration.default.extraOnsets.isEmpty)
        for json in [#"{}"#, #"{"extraOnsets":null}"#] {
            let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: Data(json.utf8))
            #expect(decoded.extraOnsets.isEmpty)
        }
        let legacy = Data(#"{"inputMethod":"telex","smartRestore":false,"schemaVersion":15}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: legacy)
        #expect(decoded.extraOnsets.isEmpty)
        #expect(decoded.inputMethod == .telex)
        #expect(!decoded.smartRestore)
        #expect(decoded.schemaVersion == FunputConfiguration.currentSchemaVersion)
    }

    @Test("Configuration round-trip retains the selection without a schema bump")
    func configurationRoundTrip() throws {
        let configuration = FunputConfiguration(inputMethod: .telex, extraOnsets: [.f, .j])
        let data = try JSONEncoder().encode(configuration)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded == configuration)
        #expect(decoded.extraOnsets == [.f, .j])
        #expect(decoded.schemaVersion == FunputConfiguration.currentSchemaVersion)
        let payload = try #require(JSONSerialization.jsonObject(with: data) as? [String: Any])
        #expect(payload["extraOnsets"] as? String == "fj")
    }

    @Test("App Group configuration storage retains extra onsets")
    func storage() throws {
        let suite = "ExtraOnsetLettersTests.\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = FunputConfigurationStore(defaults: defaults)
        let configuration = FunputConfiguration(extraOnsets: [.z, .w])
        #expect(store.save(configuration))
        #expect(store.load() == configuration)
    }
}

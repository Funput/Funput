import Foundation
import FunputShared
import KeyboardLayout
import Testing

struct KeySizingConfigurationTests {
    @Test("New installs use Funput key sizing")
    func defaultsToFunput() {
        #expect(FunputConfiguration.default.keySizing == .funput)
    }

    @Test("Schema 12 payloads keep Funput key sizing")
    func schema12KeepsFunputSizing() throws {
        let data = Data(#"{"heightScale":1.1,"schemaVersion":12}"#.utf8)
        let decoded = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(decoded.keySizing == .funput)
        #expect(decoded.heightScale == 1.1)
        #expect(decoded.schemaVersion == 14)
    }

    @Test("System key sizing survives a JSON round-trip")
    func roundTrip() throws {
        var config = FunputConfiguration.default
        config.keySizing = .system
        let data = try JSONEncoder().encode(config)
        #expect(try JSONDecoder().decode(FunputConfiguration.self, from: data) == config)
    }
}

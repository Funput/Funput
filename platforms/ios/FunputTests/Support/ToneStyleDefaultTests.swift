import Foundation
import FunputShared
import Testing

struct ToneStyleDefaultTests {
    @Test("Fresh storage uses modern tone placement")
    func freshStorage() {
        let suiteName = "app.funput.tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let configuration = FunputConfigurationStore(defaults: defaults).load()
        #expect(configuration.toneStyle == .modern)
    }

    @Test("Legacy payload without tone style stays traditional")
    func legacyPayload() throws {
        let data = Data(#"{"inputMethod":"vni","schemaVersion":12}"#.utf8)
        let configuration = try JSONDecoder().decode(FunputConfiguration.self, from: data)
        #expect(configuration.toneStyle == .traditional)
    }

    @Test("Stored tone style always wins")
    func storedToneStyle() throws {
        for style in ToneStyleOption.allCases {
            let data = Data(#"{"toneStyle":"\#(style.rawValue)"}"#.utf8)
            let configuration = try JSONDecoder().decode(FunputConfiguration.self, from: data)
            #expect(configuration.toneStyle == style)
        }
    }
}

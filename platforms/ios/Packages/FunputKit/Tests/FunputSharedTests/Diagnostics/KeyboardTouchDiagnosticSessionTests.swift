#if DEBUG
import Foundation
import FunputShared
import KeyboardLayout
import Testing

struct KeyboardTouchDiagnosticSessionTests {
    @Test("Missing pipeline mode decodes as legacy")
    func legacyCompatibility() throws {
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .vni,
            phase: .guided,
            pipelineMode: .v2,
            generation: 7
        )
        let encoded = try JSONEncoder().encode(session)
        var json = try #require(
            JSONSerialization.jsonObject(with: encoded) as? [String: Any]
        )
        json.removeValue(forKey: "pipelineMode")
        let legacy = try JSONDecoder().decode(
            KeyboardTouchDiagnosticSession.self,
            from: JSONSerialization.data(withJSONObject: json)
        )
        #expect(legacy.pipelineMode == .legacy)
    }

    @Test("Phase 3A mode decodes as V2")
    func primaryFastTapCompatibility() throws {
        let data = Data(#""primaryFastTap""#.utf8)
        let mode = try JSONDecoder().decode(
            KeyboardTouchDiagnosticPipelineMode.self,
            from: data
        )
        #expect(mode == .v2)
    }
}
#endif

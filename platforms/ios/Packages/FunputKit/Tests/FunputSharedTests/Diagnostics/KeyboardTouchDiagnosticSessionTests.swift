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
            pipelineMode: .primaryFastTap,
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
}
#endif

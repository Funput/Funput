#if DEBUG
import Foundation
import FunputShared
import KeyboardLayout
import Testing

struct KeyboardTouchDiagnosticSessionTests {
    @Test("Session round-trips without a pipeline selector")
    func roundTrip() throws {
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .vni,
            phase: .guided,
            generation: 7
        )
        let encoded = try JSONEncoder().encode(session)
        let json = try #require(
            JSONSerialization.jsonObject(with: encoded) as? [String: Any]
        )
        #expect(json["pipelineMode"] == nil)
        let decoded = try JSONDecoder().decode(
            KeyboardTouchDiagnosticSession.self,
            from: encoded
        )
        #expect(decoded == session)
    }
}
#endif

#if DEBUG
import FunputShared
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
struct KeyboardTouchAcceptanceResultTests {
    @Test("Typing and pipeline failures remain distinct")
    func classification() {
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .vni, phase: .guided, generation: 1
        )
        let typingResult = KeyboardTouchAcceptanceResult.make(
            text: "wrong",
            report: report(session, metrics: .init()),
            exactMatch: false
        )
        #expect(typingResult.classification == .typingMismatch)
        #expect(typingResult.firstMismatchIndex == 0)

        var metrics = KeyboardTouchDiagnosticMetrics()
        metrics.ownershipViolation = 1
        #expect(KeyboardTouchAcceptanceResult.make(
            text: KeyboardTouchAcceptanceFixture.expected,
            report: report(session, metrics: metrics),
            exactMatch: true
        ).classification == .pipelineRegression)
    }

    @Test("Gesture classification requires complete coverage")
    func gestureCoverage() {
        let session = KeyboardTouchDiagnosticSession(
            inputMethod: .vni, phase: .gestures, generation: 2
        )
        var metrics = KeyboardTouchDiagnosticMetrics()
        #expect(KeyboardTouchAcceptanceResult.make(
            text: "",
            report: report(session, metrics: metrics),
            exactMatch: nil,
            requiresGestureCoverage: true
        ).classification == .incompleteCoverage)

        metrics.alternateCommitted = 1
        metrics.repeatEmitted = 2
        metrics.swipeCommitted = 2
        metrics.controlCommitted = 4
        #expect(KeyboardTouchAcceptanceResult.make(
            text: "",
            report: report(session, metrics: metrics),
            exactMatch: nil,
            requiresGestureCoverage: true
        ).classification == .pass)
    }
}
#endif

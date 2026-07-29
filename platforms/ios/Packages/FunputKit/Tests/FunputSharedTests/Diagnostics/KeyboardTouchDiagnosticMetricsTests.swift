#if DEBUG
import FunputShared
import Testing

struct KeyboardTouchDiagnosticMetricsTests {
    @Test("Recovered drift is informational but excessive delay is a regression")
    func regressionThreshold() {
        var metrics = KeyboardTouchDiagnosticMetrics()
        metrics.recoveredTapSlop = 1
        metrics.emissionDelayedOver40Milliseconds = 1
        #expect(!metrics.hasShadowRegression)

        metrics.emissionDelayedOver120Milliseconds = 1
        #expect(metrics.hasShadowRegression)

        metrics = .init()
        metrics.commitGateViolation = 1
        #expect(metrics.hasShadowRegression)
    }
}
#endif

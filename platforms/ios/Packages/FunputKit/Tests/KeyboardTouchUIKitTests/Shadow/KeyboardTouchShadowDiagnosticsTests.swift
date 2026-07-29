#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

@MainActor
struct KeyboardTouchShadowDiagnosticsTests {
    @Test("Observer receives updated immutable metrics")
    func observer() {
        let (pipeline, clock) = fixture()
        var observed: [KeyboardTouchShadowMetrics] = []
        pipeline.trace.observe { observed.append($0) }

        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        #expect(observed.last?.capturedBegan == 1)
        #expect(pipeline.diagnosticState.activeContactCount == 1)
    }

    @Test("Diagnostics reset only while idle")
    func resetGuard() {
        let (pipeline, clock) = fixture()
        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        #expect(!pipeline.resetDiagnosticsIfIdle())
        #expect(pipeline.trace.metrics.capturedBegan == 1)

        consume(pipeline, clock, shadowSample(1, .cancelled, 0.1, .init(x: 10, y: 20)))
        #expect(pipeline.resetDiagnosticsIfIdle())
        #expect(pipeline.trace.metrics == KeyboardTouchShadowMetrics())
    }

    @Test("Pending comparison settles without another touch")
    func settlement() {
        let (pipeline, clock) = fixture()
        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(1, .ended, 0.1, .init(x: 10, y: 20)))
        #expect(pipeline.diagnosticState.pendingComparisonCount == 1)
        #expect(!pipeline.diagnosticState.isSettled)

        clock.advance(to: 0.221)
        #expect(pipeline.diagnosticState.pendingComparisonCount == 0)
        #expect(pipeline.diagnosticState.isSettled)
        #expect(pipeline.trace.metrics.legacyMissing == 1)
    }

    @Test("Cancellation counters retain the resolver reason")
    func cancellationReasons() {
        let (pipeline, clock) = fixture()
        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(1, .cancelled, 0.1, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(2, .began, 1, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(2, .ended, 1.1, .init(x: 27, y: 20)))
        consume(pipeline, clock, shadowSample(3, .began, 2, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(3, .ended, 2.301, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(4, .began, 3, .init(x: 0, y: 20)))
        consume(pipeline, clock, shadowSample(4, .ended, 3.1, .init(x: -13, y: 20)))

        let metrics = pipeline.trace.metrics
        #expect(metrics.cancelledSystem == 1)
        #expect(metrics.cancelledTapSlop == 0)
        #expect(metrics.recoveredTapSlop == 1)
        #expect(metrics.cancelledDuration == 1)
        #expect(metrics.cancelledOutside == 1)
        #expect(metrics.shadowCancelled == 3)
    }

    @Test("Telemetry measures arbiter bypass and terminal emission delay")
    func arbiterTelemetry() {
        let (pipeline, clock) = fixture()
        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(2, .began, 0.01, .init(x: 60, y: 20)))
        consume(pipeline, clock, shadowSample(2, .ended, 0.02, .init(x: 60, y: 20)))

        clock.advance(to: 0.061)

        let metrics = pipeline.trace.metrics
        #expect(metrics.maximumConcurrentContacts == 2)
        #expect(metrics.maximumArbiterDepth == 2)
        #expect(metrics.arbiterBypassCount == 1)
        #expect(metrics.maximumEmissionDelayMilliseconds == 41)
        #expect(metrics.emissionDelayedOver40Milliseconds == 1)
        #expect(metrics.emissionDelayedOver120Milliseconds == 0)
    }

    @Test("Unknown sources and out-of-scope callbacks remain distinct")
    func callbackSources() {
        let (pipeline, clock) = fixture()
        consume(pipeline, clock, shadowSample(99, .moved, 0, .init(x: 10, y: 20)))
        pipeline.recordUnknownCaptureCallback()

        let backspace = KeySpec(id: "delete", label: "", role: .backspace)
        pipeline.updateGeometry(ResolvedKeyboard(
            size: .init(width: 50, height: 50),
            toolbarFrame: nil,
            rows: [[ResolvedKey(
                spec: backspace,
                frame: .init(x: 0, y: 0, width: 50, height: 50)
            )]]
        ))
        consume(pipeline, clock, shadowSample(1, .began, 1, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(1, .ended, 1.1, .init(x: 10, y: 20)))

        let metrics = pipeline.trace.metrics
        #expect(metrics.captureUnknownCallback == 1)
        #expect(metrics.resolverUnknownCallback == 1)
        #expect(metrics.outOfScopeCallback == 1)
        #expect(metrics.unknownCallback == 2)
    }

    @Test("Gesture promotion removes the contact from shadow comparison")
    func gesturePromotion() {
        let (pipeline, clock) = fixture()
        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        pipeline.promoteToLegacy(1)
        consume(pipeline, clock, shadowSample(1, .ended, 0.4, .init(x: 10, y: 20)))

        #expect(pipeline.trace.metrics.shadowResolved == 0)
        #expect(pipeline.trace.metrics.outOfScopeCallback == 1)
        #expect(pipeline.diagnosticState.activeContactCount == 0)
    }

    private func fixture() -> (KeyboardTouchShadowPipeline, ShadowTestClock) {
        let clock = ShadowTestClock()
        let pipeline = KeyboardTouchShadowPipeline(
            clock: { clock.now },
            schedule: { delay, action in clock.schedule(delay: delay, action: action) }
        )
        pipeline.updateGeometry(shadowGeometry().0)
        return (pipeline, clock)
    }

    private func consume(
        _ pipeline: KeyboardTouchShadowPipeline,
        _ clock: ShadowTestClock,
        _ sample: ContactSample
    ) {
        clock.now = sample.timestamp
        pipeline.consume(sample)
    }
}
#endif

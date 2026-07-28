#if canImport(UIKit)
import CoreGraphics
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

@MainActor
@Suite("Touch shadow comparison")
struct KeyboardTouchShadowComparatorTests {
    @Test func missingActionCanArriveLateWithoutBecomingDuplicate() {
        let (pipeline, clock, a, _) = fixture()
        tap(pipeline, clock, id: 1, point: .init(x: 10, y: 20), at: 0)
        clock.advance(to: 0.221)
        #expect(pipeline.trace.metrics.legacyMissing == 1)

        pipeline.recordLegacyRelease(a)
        #expect(pipeline.trace.metrics.legacyLate == 1)
        #expect(pipeline.trace.metrics.orderMismatch == 0)
    }

    @Test func reverseLegacyOrderIsReported() {
        let (pipeline, clock, a, b) = fixture()
        tap(pipeline, clock, id: 1, point: .init(x: 10, y: 20), at: 0)
        tap(pipeline, clock, id: 2, point: .init(x: 70, y: 20), at: 0.11)
        pipeline.recordLegacyRelease(b)
        pipeline.recordLegacyRelease(a)
        #expect(pipeline.trace.metrics.orderMismatch == 1)
    }

    @Test func timestampTieIsNotReportedAsReorder() {
        let (pipeline, clock, a, b) = fixture()
        send(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        send(pipeline, clock, shadowSample(2, .began, 0, .init(x: 70, y: 20)))
        send(pipeline, clock, shadowSample(1, .ended, 0.1, .init(x: 10, y: 20)))
        send(pipeline, clock, shadowSample(2, .ended, 0.1, .init(x: 70, y: 20)))
        pipeline.recordLegacyRelease(a)
        pipeline.recordLegacyRelease(b)

        #expect(pipeline.trace.metrics.timestampTie == 2)
        #expect(pipeline.trace.metrics.orderMismatch == 0)
    }

    @Test func reversedTimestampTieRemainsInconclusive() {
        let (pipeline, clock, a, b) = fixture()
        send(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        send(pipeline, clock, shadowSample(2, .began, 0, .init(x: 70, y: 20)))
        send(pipeline, clock, shadowSample(1, .ended, 0.1, .init(x: 10, y: 20)))
        send(pipeline, clock, shadowSample(2, .ended, 0.1, .init(x: 70, y: 20)))
        pipeline.recordLegacyRelease(b)
        pipeline.recordLegacyRelease(a)

        #expect(pipeline.trace.metrics.timestampTie == 1)
        #expect(pipeline.trace.metrics.orderMismatch == 0)
    }

    @Test func layoutChangeWhileActiveUsesDiagnosticOnly() {
        let (pipeline, clock, _, _) = fixture()
        send(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        pipeline.updateGeometry(shadowGeometry().0)
        #expect(pipeline.activeContactCount == 1)
        #expect(pipeline.trace.metrics.layoutChangedWhileActive == 0)

        var changed = shadowGeometry().0
        changed = .init(size: .init(width: 101, height: 50), toolbarFrame: nil, rows: changed.rows)
        pipeline.updateGeometry(changed)
        #expect(pipeline.trace.metrics.layoutChangedWhileActive == 1)
    }

    private func fixture() -> (KeyboardTouchShadowPipeline, ShadowTestClock, KeySpec, KeySpec) {
        let clock = ShadowTestClock()
        let pipeline = KeyboardTouchShadowPipeline(
            clock: { clock.now },
            schedule: { delay, action in clock.schedule(delay: delay, action: action) }
        )
        let (geometry, a, b) = shadowGeometry()
        pipeline.updateGeometry(geometry)
        return (pipeline, clock, a, b)
    }

    private func tap(
        _ pipeline: KeyboardTouchShadowPipeline,
        _ clock: ShadowTestClock,
        id: UInt64,
        point: CGPoint,
        at timestamp: TimeInterval
    ) {
        send(pipeline, clock, shadowSample(id, .began, timestamp, point))
        send(pipeline, clock, shadowSample(id, .ended, timestamp + 0.1, point))
    }

    private func send(
        _ pipeline: KeyboardTouchShadowPipeline,
        _ clock: ShadowTestClock,
        _ sample: ContactSample
    ) {
        clock.now = sample.timestamp
        pipeline.consume(sample)
    }
}
#endif

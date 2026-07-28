#if canImport(UIKit)
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

@MainActor
@Suite("Touch shadow ordering")
struct KeyboardTouchShadowOrderingTests {
    @Test func reverseReleaseStillMatchesIntentOrder() {
        let clock = ShadowTestClock()
        let pipeline = makePipeline(clock)
        let (geometry, a, b) = shadowGeometry()
        pipeline.updateGeometry(geometry)

        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(2, .began, 0.01, .init(x: 70, y: 20)))
        consume(pipeline, clock, shadowSample(2, .ended, 0.02, .init(x: 70, y: 20)))
        consume(pipeline, clock, shadowSample(1, .ended, 0.03, .init(x: 10, y: 20)))
        pipeline.recordLegacyRelease(a)
        pipeline.recordLegacyRelease(b)

        #expect(pipeline.trace.metrics.matched == 2)
        #expect(pipeline.trace.metrics.orderMismatch == 0)
    }

    @Test func deadlineReleasesFollowerWithoutAnotherSample() {
        let clock = ShadowTestClock()
        let pipeline = makePipeline(clock)
        let (geometry, _, b) = shadowGeometry()
        pipeline.updateGeometry(geometry)

        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(2, .began, 0.01, .init(x: 70, y: 20)))
        consume(pipeline, clock, shadowSample(2, .ended, 0.02, .init(x: 70, y: 20)))
        clock.advance(to: 0.061)
        pipeline.recordLegacyRelease(b)

        #expect(pipeline.trace.metrics.shadowResolved == 1)
        #expect(pipeline.trace.metrics.matched == 1)
    }

    @Test func cancellationNeverResolves() {
        let clock = ShadowTestClock()
        let pipeline = makePipeline(clock)
        pipeline.updateGeometry(shadowGeometry().0)
        consume(pipeline, clock, shadowSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(pipeline, clock, shadowSample(1, .cancelled, 0.1, .init(x: 10, y: 20)))

        #expect(pipeline.trace.metrics.shadowResolved == 0)
        #expect(pipeline.trace.metrics.shadowCancelled == 1)
    }

    private func makePipeline(_ clock: ShadowTestClock) -> KeyboardTouchShadowPipeline {
        KeyboardTouchShadowPipeline(
            clock: { clock.now },
            schedule: { delay, action in clock.schedule(delay: delay, action: action) }
        )
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

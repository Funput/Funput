#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

@MainActor
struct KeyboardTouchPipelineTests {
    @Test("Reverse release and deadline preserve bounded ordering")
    func orderingAndProgress() {
        let fixture = makePipeline()
        consume(fixture, touchSample(1, .began, 0, .init(x: 10, y: 20)))
        consume(fixture, touchSample(2, .began, 0.01, .init(x: 60, y: 20)))
        consume(fixture, touchSample(2, .ended, 0.02, .init(x: 60, y: 20)))
        #expect(fixture.emissions.keys.isEmpty)
        consume(fixture, touchSample(1, .ended, 0.03, .init(x: 10, y: 20)))
        #expect(fixture.emissions.keys == ["a", "b"])

        fixture.pipeline.reset()
        fixture.emissions.keys.removeAll()
        fixture.pipeline.updateGeometry(touchGeometry().0)
        consume(fixture, touchSample(3, .began, 1, .init(x: 10, y: 20)))
        consume(fixture, touchSample(4, .began, 1.01, .init(x: 60, y: 20)))
        consume(fixture, touchSample(4, .ended, 1.02, .init(x: 60, y: 20)))
        fixture.clock.advance(to: 1.061)
        #expect(fixture.emissions.keys == ["b"])
    }

    @Test("Text drift resolves while duration and outside hand back")
    func resolutionPolicy() {
        let fixture = makePipeline()
        consume(fixture, touchSample(1, .began, 0, .init(x: 10, y: 20)))
        let drift = consume(
            fixture,
            touchSample(1, .ended, 0.1, .init(x: 60, y: 20))
        )
        if case let .resolved(_, metadata) = drift {
            #expect(metadata.exceededTapSlop)
        } else {
            Issue.record("Expected recovered fast tap")
        }
        #expect(fixture.emissions.keys == ["b"])

        consume(fixture, touchSample(2, .began, 1, .init(x: 10, y: 20)))
        let long = consume(
            fixture,
            touchSample(2, .ended, 1.301, .init(x: 10, y: 20))
        )
        if case .fallback(_, .exceededDuration) = long {} else {
            Issue.record("Expected duration fallback")
        }
    }

    @Test("Promotion and system cancellation never emit")
    func cancellation() {
        let fixture = makePipeline()
        consume(fixture, touchSample(1, .began, 0, .init(x: 10, y: 20)))
        #expect(fixture.pipeline.exclude(.init(rawValue: 1), at: 0.1))
        consume(fixture, touchSample(1, .ended, 0.2, .init(x: 10, y: 20)))

        consume(fixture, touchSample(2, .began, 1, .init(x: 10, y: 20)))
        let cancelled = consume(
            fixture,
            touchSample(2, .cancelled, 1.1, .init(x: 10, y: 20))
        )
        if case .cancelled = cancelled {} else {
            Issue.record("Expected system cancellation")
        }
        #expect(fixture.emissions.keys.isEmpty)
    }

    private func makePipeline() -> PipelineFixture {
        let clock = TouchTestClock()
        let emissions = EmissionBox()
        let pipeline = KeyboardTouchPipeline(
            eligibleRoles: [.character, .vniModifier, .punctuation],
            recoveringTapSlopRoles: [.character, .vniModifier, .punctuation],
            clock: { clock.now },
            schedule: { delay, action in clock.schedule(delay: delay, action: action) },
            onEmit: { emissions.keys.append($0.payload.hit.key.id) }
        )
        pipeline.updateGeometry(touchGeometry().0)
        return PipelineFixture(pipeline: pipeline, clock: clock, emissions: emissions)
    }

    @discardableResult
    private func consume(
        _ fixture: PipelineFixture,
        _ sample: ContactSample
    ) -> KeyboardTouchDisposition {
        fixture.clock.now = sample.timestamp
        return fixture.pipeline.consume(sample)
    }
}

@MainActor
private struct PipelineFixture {
    let pipeline: KeyboardTouchPipeline
    let clock: TouchTestClock
    let emissions: EmissionBox
}

private final class EmissionBox {
    var keys: [String] = []
}
#endif

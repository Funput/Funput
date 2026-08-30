#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

@MainActor
struct KeyboardTouchPipelineTests {
    @Test("Reverse release and deadline preserve bounded ordering")
    func orderingAndProgress() {
        let fixture = makeTouchPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 10, y: 20)))
        fixture.consume(touchSample(2, .began, 0.01, .init(x: 60, y: 20)))
        fixture.consume(touchSample(2, .ended, 0.02, .init(x: 60, y: 20)))
        #expect(fixture.emissions.keys.isEmpty)
        fixture.consume(touchSample(1, .ended, 0.03, .init(x: 10, y: 20)))
        #expect(fixture.emissions.keys == ["a", "b"])

        fixture.pipeline.reset()
        fixture.emissions.keys.removeAll()
        fixture.pipeline.updateGeometry(touchGeometry().0)
        fixture.consume(touchSample(3, .began, 1, .init(x: 10, y: 20)))
        fixture.consume(touchSample(4, .began, 1.01, .init(x: 60, y: 20)))
        fixture.consume(touchSample(4, .ended, 1.02, .init(x: 60, y: 20)))
        fixture.clock.advance(to: 1.061)
        #expect(fixture.emissions.keys == ["b"])
    }

    /// Drift keeps the press alive and keeps the key it landed on: the slop is exceeded, so
    /// the recovery policy is what saves it, and the resolver is what decides it is still `a`.
    @Test("Text drift resolves to the key it landed on while duration hands back")
    func resolutionPolicy() {
        let fixture = makeTouchPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 10, y: 20)))
        let drift = fixture.consume(
            touchSample(1, .ended, 0.1, .init(x: 60, y: 20))
        )
        if case let .resolved(_, metadata) = drift {
            #expect(metadata.exceededTapSlop)
        } else {
            Issue.record("Expected recovered fast tap")
        }
        #expect(fixture.emissions.keys == ["a"])

        fixture.consume(touchSample(2, .began, 1, .init(x: 10, y: 20)))
        let long = fixture.consume(
            touchSample(2, .ended, 1.301, .init(x: 10, y: 20))
        )
        if case .fallback(_, .exceededDuration) = long {} else {
            Issue.record("Expected duration fallback")
        }
    }

    @Test("Promotion and system cancellation never emit")
    func cancellation() {
        let fixture = makeTouchPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 10, y: 20)))
        #expect(fixture.pipeline.exclude(.init(rawValue: 1), at: 0.1))
        fixture.consume(touchSample(1, .ended, 0.2, .init(x: 10, y: 20)))

        fixture.consume(touchSample(2, .began, 1, .init(x: 10, y: 20)))
        let cancelled = fixture.consume(
            touchSample(2, .cancelled, 1.1, .init(x: 10, y: 20))
        )
        if case .cancelled = cancelled {} else {
            Issue.record("Expected system cancellation")
        }
        #expect(fixture.emissions.keys.isEmpty)
    }
}
#endif

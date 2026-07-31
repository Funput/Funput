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

    @Test("Text drift resolves while duration and outside hand back")
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
        #expect(fixture.emissions.keys == ["b"])

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

#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import KeyboardTouchCore
import Testing

@testable import KeyboardTouchUIKit

/// The evidence a press hands typo correction is measured where the finger landed.
@MainActor
struct KeyboardTouchPipelineEvidenceTests {
    /// `g` and `h` side by side, 40pt wide on a 50pt pitch.
    private func geometry() -> ResolvedKeyboard {
        let keys = ["g", "h"].enumerated().map { index, label in
            ResolvedKey(
                spec: KeySpec(id: label, label: label, role: .character),
                frame: CGRect(x: CGFloat(index) * 50, y: 0, width: 40, height: 40)
            )
        }
        return ResolvedKeyboard(
            size: CGSize(width: 100, height: 40),
            toolbarFrame: nil,
            rows: [keys],
            pitch: CGSize(width: 50, height: 50)
        )
    }

    @Test("A slide from the edge of g to the centre of h is reported from where it landed")
    func measuredAtTouchDown() throws {
        var released: [KeyboardTouchEvidence?] = []
        let clock = TouchTestClock()
        let pipeline = KeyboardTouchPipeline(
            policy: .recoveringAll([.character]),
            clock: { clock.now },
            schedule: { delay, action in clock.schedule(delay: delay, action: action) },
            onEmit: { emission in
                if case let .released(_, evidence) = emission.payload { released.append(evidence) }
            }
        )
        pipeline.updateGeometry(geometry())

        // Down inside `g`, two points from its edge; up dead on `h`'s centre.
        pipeline.consume(touchSample(1, .began, 0, .init(x: 38, y: 20)))
        pipeline.consume(touchSample(1, .ended, 0.05, .init(x: 70, y: 20)))

        let evidence = try #require(released.first ?? nil)
        #expect(evidence.typed == "h", "the lift still decides the key")
        #expect(abs(evidence.typedDistance - 0.64) < 0.01, "but the distance is from the landing")
        #expect(evidence.first?.scalar == "g")
        #expect(abs((evidence.first?.distance ?? 1) - 0.36) < 0.01)
    }

    @Test("A landing is forgotten with its contact")
    func forgotten() {
        let fixture = makeTouchPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 10, y: 20)))
        fixture.consume(touchSample(1, .ended, 0.05, .init(x: 10, y: 20)))
        fixture.consume(touchSample(2, .began, 1, .init(x: 10, y: 20)))
        fixture.pipeline.reset()

        #expect(fixture.pipeline.initialLocations.isEmpty)
    }
}
#endif

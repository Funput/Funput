#if canImport(UIKit)
import CoreGraphics
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing

/// `n` sits on the letter row (y 0...40) directly above the spacebar (y 50...90).
@MainActor
struct KeyboardTouchPipelineSwipeLockTests {
    @Test("A spacebar press that drifts up into the letter row still types a space")
    func spacebarKeepsItsKey() {
        let fixture = makeSwipeLockPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 50, y: 70)))
        fixture.consume(touchSample(1, .moved, 0.03, .init(x: 50, y: 30)))
        fixture.consume(touchSample(1, .ended, 0.05, .init(x: 50, y: 30)))

        #expect(fixture.emissions.keys == ["space"])
    }

    @Test("A letter press that slides onto the spacebar types the space under the lift")
    func letterFollowsTheLift() {
        let fixture = makeSwipeLockPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 50, y: 20)))
        fixture.consume(touchSample(1, .ended, 0.05, .init(x: 50, y: 70)))

        #expect(fixture.emissions.keys == ["space"])
    }

    @Test("A spacebar press that lifts off the keys still types a space")
    func spacebarRecoversReleaseOutside() {
        let fixture = makeSwipeLockPipeline()
        fixture.consume(touchSample(1, .began, 0, .init(x: 50, y: 70)))
        fixture.consume(touchSample(1, .ended, 0.05, .init(x: 50, y: 140)))

        #expect(fixture.emissions.keys == ["space"])
    }

    private func makeSwipeLockPipeline() -> PipelineFixture {
        let fixture = makeTouchPipeline(policy: .recoveringAll([.character, .space]))
        let n = KeySpec(id: "n", label: "n", role: .character)
        let space = KeySpec(
            id: "space",
            label: "",
            role: .space,
            horizontalSwipeAction: .toggleLanguage
        )
        fixture.pipeline.updateGeometry(
            ResolvedKeyboard(
                size: CGSize(width: 100, height: 90),
                toolbarFrame: nil,
                rows: [
                    [ResolvedKey(spec: n, frame: CGRect(x: 0, y: 0, width: 100, height: 40))],
                    [ResolvedKey(spec: space, frame: CGRect(x: 0, y: 50, width: 100, height: 40))],
                ]
            )
        )
        return fixture
    }
}
#endif

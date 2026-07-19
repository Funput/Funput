@testable import KeyboardRenderer
import Foundation
import Testing

struct KeyboardKeyContentLayoutTests {
    @Test(
        "Hints stay above the unchanged primary-label frame",
        arguments: [CGSize(width: 34, height: 42), CGSize(width: 52, height: 60)]
    )
    func hintDoesNotMovePrimaryLabel(size: CGSize) {
        let bounds = CGRect(origin: .zero, size: size)
        let frames = KeyboardKeyContentGeometry.frames(in: bounds, hintLineHeight: 11)
        let expectedPrimary = bounds.insetBy(
            dx: 5,
            dy: max(6, bounds.height * 0.2) * 0.5
        )

        #expect(frames.primaryLabel == expectedPrimary)
        #expect(frames.hint.midY < frames.primaryLabel.midY)
        #expect(frames.hint.minY == 3)
        #expect(frames.hint.maxX == bounds.maxX - 4)
    }
}

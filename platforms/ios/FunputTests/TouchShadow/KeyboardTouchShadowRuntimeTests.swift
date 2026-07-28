#if DEBUG
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

final class RuntimeShadowTouch: UITouch {
    var testTimestamp: TimeInterval = 0
    var testLocation: CGPoint = .zero

    override var timestamp: TimeInterval { testTimestamp }
    override func location(in view: UIView?) -> CGPoint { testLocation }
    override func previousLocation(in view: UIView?) -> CGPoint { testLocation }
}

@MainActor
struct KeyboardTouchShadowRuntimeTests {
    @Test func reverseReleaseKeepsLegacyOutputAndMatchesShadow() {
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.isKeySoundEnabled = false
        presentation.showsKeyPreviews = false
        let surface = KeyboardSurfaceView(presentation: presentation)
        surface.frame = CGRect(x: 0, y: 0, width: 390, height: 304)
        surface.layoutIfNeeded()

        let textKeys = surface.resolvedGeometry().keys.filter {
            $0.spec.role == .character || $0.spec.role == .vniModifier
        }
        let first = makeTouch(textKeys[0].frame.center, timestamp: 1)
        let second = makeTouch(textKeys[1].frame.center, timestamp: 1.01)
        var releasedIDs: [String] = []
        surface.onKeyEvent = { event in
            if event.phase == .released { releasedIDs.append(event.key.id) }
        }

        surface.touchOverlay.touchesBegan([first], with: nil)
        surface.touchOverlay.touchesBegan([second], with: nil)
        second.testTimestamp = 1.02
        surface.touchOverlay.touchesEnded([second], with: nil)
        first.testTimestamp = 1.03
        surface.touchOverlay.touchesEnded([first], with: nil)

        #expect(releasedIDs == [textKeys[0].spec.id, textKeys[1].spec.id])
        #expect(surface.touchShadowResolvedCount == 2)
        #expect(surface.touchShadowMatchCount == 2)
    }

    private func makeTouch(_ point: CGPoint, timestamp: TimeInterval) -> RuntimeShadowTouch {
        let touch = RuntimeShadowTouch()
        touch.testLocation = point
        touch.testTimestamp = timestamp
        return touch
    }
}

private extension CGRect {
    var center: CGPoint { CGPoint(x: midX, y: midY) }
}
#endif

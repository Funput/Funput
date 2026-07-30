#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import KeyboardTouchCore
import Testing

@MainActor
struct KeyboardTouchPipelineModeTests {
    @Test("Mode request waits until active controller contact ends")
    func deferredSwitch() {
        let surface = KeyboardSurfaceView()
        #expect(surface.setTouchPipelineMode(.v2))
        let key = KeySpec(id: "a", label: "a", role: .character)
        surface.interactionController.beginTouch(
            token: 1,
            key: key,
            point: .zero,
            sourceFrame: nil,
            presentation: surface.presentation
        )

        #expect(!surface.setTouchPipelineMode(.legacy))
        #expect(surface.touchPipelineMode == .v2)
        surface.interactionController.endTouch(token: 1)
        #expect(surface.applyPendingTouchPipelineModeIfIdle())
        #expect(surface.touchPipelineMode == .legacy)
    }

    @Test("Surface defaults to V2")
    func defaultMode() {
        #expect(KeyboardSurfaceView().touchPipelineMode == .v2)
    }

    @Test("Primary overlay uses captured contact ID without reconciliation")
    func primaryCapture() {
        let recorder = OverlayTouchRecorder()
        var samples: [ContactSample] = []
        recorder.overlay.onSamples = { samples.append(contentsOf: $0) }
        recorder.overlay.setPipelineMode(.v2)
        let touch = recorder.touch(at: OverlayTouchRecorder.keyAPoint)

        recorder.began(touch, at: 0)
        recorder.ended(touch, at: 0.1)

        #expect(samples.map(\.phase) == [.began, .ended])
        #expect(Set(samples.map(\.id)).count == 1)
        #expect(recorder.log == [
            "begin(1,key-a)", "move(1,key-a)", "end(1)",
        ])
        #expect(recorder.lastReconcile.isEmpty)
    }
}
#endif

#if canImport(UIKit)
import KeyboardTouchCore
import KeyboardTouchUIKit
import Testing
import UIKit

@MainActor
@Suite("UIKit touch capture")
struct UIKitTouchCaptureAdapterTests {
    @Test func copiesTouchValuesAndKeepsIdentityUntilTerminal() {
        let adapter = UIKitTouchCaptureAdapter()
        let view = UIView()
        let touch = ShadowStubTouch()
        touch.stubTimestamp = 1
        touch.stubLocation = CGPoint(x: 10, y: 20)
        touch.stubPreviousLocation = CGPoint(x: 9, y: 19)

        let began = adapter.samples(for: [touch], phase: .began, in: view)
        touch.stubTimestamp = 2
        touch.stubLocation = CGPoint(x: 12, y: 22)
        let moved = adapter.samples(for: [touch], phase: .moved, in: view)
        let ended = adapter.samples(for: [touch], phase: .ended, in: view)

        #expect(began.count == 1)
        #expect(began[0].phase == .began)
        #expect(began[0].timestamp == 1)
        #expect(began[0].location == CGPoint(x: 10, y: 20))
        #expect(began[0].previousLocation == CGPoint(x: 9, y: 19))
        #expect(moved[0].id == began[0].id)
        #expect(ended[0].id == began[0].id)
    }

    @Test func recycledObjectRetiresOldIDAndNeverReusesIt() {
        let adapter = UIKitTouchCaptureAdapter()
        let view = UIView()
        let touch = ShadowStubTouch()
        let first = adapter.samples(for: [touch], phase: .began, in: view)
        let second = adapter.samples(for: [touch], phase: .began, in: view)

        #expect(second.count == 2)
        #expect(second[0].phase == .cancelled)
        #expect(second[0].id == first[0].id)
        #expect(second[1].phase == .began)
        #expect(second[1].id != first[0].id)
    }

    @Test func unknownCallbacksAreCountedWithoutSamples() {
        let adapter = UIKitTouchCaptureAdapter()
        let touch = ShadowStubTouch()
        #expect(adapter.samples(for: [touch], phase: .moved, in: UIView()).isEmpty)
        #expect(adapter.samples(for: [touch], phase: .cancelled, in: UIView()).isEmpty)
        #expect(adapter.unknownCallbackCount == 2)
    }

    @Test func batchDoesNotLoseContacts() {
        let adapter = UIKitTouchCaptureAdapter()
        let touches: Set<UITouch> = [ShadowStubTouch(), ShadowStubTouch(), ShadowStubTouch()]
        let samples = adapter.samples(for: touches, phase: .began, in: UIView())
        #expect(samples.count == 3)
        #expect(Set(samples.map(\.id)).count == 3)
    }
}
#endif

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

    /// Retiring the old contact is a policy choice, not an observation. It is reported so the
    /// field data can say whether UIKit actually recycles a `UITouch` this way — the answer
    /// decides whether the policy stays.
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
        #expect(adapter.staleIdentityCount == 1)
    }

    @Test func aPlainSequenceReportsNoStaleIdentity() {
        let adapter = UIKitTouchCaptureAdapter()
        let view = UIView()
        let touch = ShadowStubTouch()
        _ = adapter.samples(for: [touch], phase: .began, in: view)
        _ = adapter.samples(for: [touch], phase: .ended, in: view)

        #expect(adapter.staleIdentityCount == 0)
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

    /// `Set` iteration order is unspecified, so a batch has to be sorted before contact IDs are
    /// assigned. This does not recover the physical press order for identical timestamps — it
    /// only makes a run reproducible.
    @Test func batchIsOrderedByTimestampThenPosition() {
        let touches = (0..<3).map { index -> ShadowStubTouch in
            let touch = ShadowStubTouch()
            touch.stubTimestamp = index == 0 ? 2 : 1
            touch.stubLocation = CGPoint(x: index == 1 ? 30 : 10, y: 0)
            return touch
        }
        let view = UIView()

        let samples = UIKitTouchCaptureAdapter()
            .samples(for: Set(touches), phase: .began, in: view)
        let repeated = UIKitTouchCaptureAdapter()
            .samples(for: Set(touches.reversed()), phase: .began, in: view)

        // Timestamp 1 first; the tie breaks on x, so (10, 0) precedes (30, 0).
        #expect(samples.map(\.timestamp) == [1, 1, 2])
        #expect(samples.map(\.location.x) == [10, 30, 10])
        #expect(samples.map(\.location) == repeated.map(\.location))
    }
}
#endif

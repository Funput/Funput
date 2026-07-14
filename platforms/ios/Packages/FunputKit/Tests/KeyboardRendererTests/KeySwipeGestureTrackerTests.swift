#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import KeyboardLayout
import Testing

struct KeySwipeGestureTrackerTests {
    @Test("Short movement remains a tap")
    func belowThreshold() {
        var tracker = KeySwipeGestureTracker()

        let action = tracker.update(
            translation: CGPoint(x: 31, y: 0),
            action: .toggleLanguage
        )

        #expect(action == nil)
    }

    @Test("Left and right horizontal swipes toggle language")
    func horizontalDirections() {
        var right = KeySwipeGestureTracker()
        var left = KeySwipeGestureTracker()

        #expect(right.update(
            translation: CGPoint(x: 40, y: 4),
            action: .toggleLanguage
        ) == .toggleLanguage)
        #expect(left.update(
            translation: CGPoint(x: -40, y: 4),
            action: .toggleLanguage
        ) == .toggleLanguage)
    }

    @Test("Vertical movement does not trigger a horizontal action")
    func verticalDominance() {
        var tracker = KeySwipeGestureTracker()

        let action = tracker.update(
            translation: CGPoint(x: 40, y: 36),
            action: .toggleLanguage
        )

        #expect(action == nil)
    }

    @Test("One gesture emits at most one action")
    func emitsOnce() {
        var tracker = KeySwipeGestureTracker()

        let first = tracker.update(
            translation: CGPoint(x: 40, y: 0),
            action: .toggleLanguage
        )
        let second = tracker.update(
            translation: CGPoint(x: 80, y: 0),
            action: .toggleLanguage
        )

        #expect(first == .toggleLanguage)
        #expect(second == nil)
    }
}
#endif

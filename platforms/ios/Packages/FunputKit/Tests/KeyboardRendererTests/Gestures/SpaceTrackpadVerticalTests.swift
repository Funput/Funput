#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import KeyboardLayout
import Testing

/// The vertical half of the spacebar trackpad. The horizontal half, and everything the two
/// share about arming and claiming, lives in ``SpaceTrackpadGestureTests``.
@MainActor
struct SpaceTrackpadVerticalTests {
    private static let spaceKey = KeySpec(
        id: "space",
        label: "Tiếng Việt",
        role: .space,
        horizontalSwipeAction: .toggleLanguage
    )
    private static let hold = 0.35
    /// One line per 24pt, matching `SpaceCursorPanTracker`'s default step height.
    private static let lineStep: CGFloat = 24

    @Test("Holding then dragging straight up moves the caret a line at a time")
    func straightUpMovesLines() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 120, y: 220 - 2 * Self.lineStep)

        #expect(subject.claims == [.trackpad])
        #expect(subject.phases == [.pressed, .cursorMoved(.init(lines: -2))])
    }

    @Test("A diagonal drag reports both axes in one step")
    func diagonalReportsBothAxes() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 150, y: 220 + Self.lineStep)

        #expect(subject.phases == [.pressed, .cursorMoved(.init(columns: 3, lines: 1))])
    }

    @Test("Reversing vertically costs nothing and does not drift")
    func verticalReversalDoesNotDrift() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 120, y: 220 + 2 * Self.lineStep)
        subject.move(to: 120, y: 220)

        #expect(subject.phases == [
            .pressed,
            .cursorMoved(.init(lines: 2)),
            .cursorMoved(.init(lines: -2)),
        ])
    }

    @Test("A quick upward flick is not a trackpad drag")
    func quickFlickDoesNotActivate() {
        let subject = subject()
        subject.begin()
        subject.move(to: 120, y: 220 - 3 * Self.lineStep)
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 120, y: 220 - 4 * Self.lineStep)

        #expect(subject.claims.isEmpty)
        #expect(!subject.phases.contains { if case .cursorMoved = $0 { true } else { false } })
    }

    private func subject() -> GestureTestSubject {
        GestureTestSubject(key: Self.spaceKey)
    }
}
#endif

#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardAlternateInteractionTests {
    @Test("Hold and drag emits one alternate without releasing the base key")
    func selectAlternate() throws {
        let subject = Subject()
        subject.begin()
        subject.scheduler.runNext()
        let layout = try #require(subject.palette)
        let cell = layout.itemFrames[1].offsetBy(dx: layout.frame.minX, dy: layout.frame.minY)
        subject.move(to: CGPoint(x: cell.midX, y: cell.midY))
        subject.controller.endTouch(token: 1)

        #expect(subject.events.count == 2)
        #expect(subject.events[0].phase == .pressed)
        guard case let .alternateSelected(value) = subject.events[1].phase else {
            Issue.record("Expected alternate selection")
            return
        }
        #expect(value.text == "á")
    }

    @Test("Quick tap and early drag keep normal key semantics")
    func normalTouches() {
        let quick = Subject()
        quick.begin()
        quick.controller.endTouch(token: 1)
        #expect(quick.events.map(\.phase) == [.pressed, .released])

        let dragged = Subject()
        dragged.begin()
        dragged.move(to: CGPoint(x: 140, y: 220))
        dragged.scheduler.runNext()
        dragged.controller.endTouch(token: 1)
        #expect(dragged.events.map(\.phase) == [.pressed, .released])
        #expect(dragged.palette == nil)
    }

    @Test("Leaving the palette cancels the alternate release")
    func cancelOutside() {
        let subject = Subject()
        subject.begin()
        subject.scheduler.runNext()
        subject.move(to: CGPoint(x: 389, y: 303))
        subject.controller.endTouch(token: 1)
        #expect(subject.events.map(\.phase) == [.pressed, .cancelled])
    }
}

@MainActor
private final class Subject {
    let scheduler = TestRepeatScheduler()
    var events: [KeyboardKeyEvent] = []
    var palette: KeyboardAlternatePaletteLayout?
    lazy var controller = KeyboardSurfaceInteractionController(
        onEvent: { [weak self] in self?.events.append($0) },
        onPreview: { _, _ in },
        onAlternatePreview: { [weak self] _, layout, _ in self?.palette = layout },
        repeatScheduler: scheduler.schedule
    )
    let key = KeySpec(
        id: "a",
        label: "a",
        role: .character,
        shiftedLabel: "A",
        alternates: VietnameseKeyAlternates.values(for: "a")
    )
    var presentation: KeyboardPresentation {
        var value = KeyboardPresentation()
        value.isHapticFeedbackEnabled = false
        return value
    }

    func begin() {
        controller.beginTouch(
            token: 1,
            key: key,
            point: CGPoint(x: 120, y: 220),
            sourceFrame: CGRect(x: 102, y: 198, width: 36, height: 44),
            containerBounds: CGRect(x: 0, y: 0, width: 390, height: 304),
            presentation: presentation
        )
    }

    func move(to point: CGPoint) {
        controller.moveTouch(
            token: 1,
            key: key,
            point: point,
            sourceFrame: CGRect(x: 102, y: 198, width: 36, height: 44),
            presentation: presentation
        )
    }
}
#endif

import KeyboardLayout
import KeyboardRenderer
import XCTest

@MainActor
final class SpaceRepeatSurfaceTests: XCTestCase {
    func testHoldingSpaceRepeatsAndSuppressesRelease() async throws {
        let subject = try makeSubject()

        subject.interaction.sendActions(for: .touchDown)
        try await Task.sleep(for: .milliseconds(460))
        subject.interaction.sendActions(for: .touchUpInside)

        let spaceEvents = subject.events.value.filter { $0.key.role == .space }
        XCTAssertEqual(spaceEvents.first?.phase, .pressed)
        XCTAssertTrue(spaceEvents.contains { $0.phase == .repeated })
        XCTAssertFalse(spaceEvents.contains { $0.phase == .released })
    }

    func testTappingSpaceCommitsOnceAndDoesNotRepeatLater() async throws {
        let subject = try makeSubject()

        subject.interaction.sendActions(for: .touchDown)
        subject.interaction.sendActions(for: .touchUpInside)
        try await Task.sleep(for: .milliseconds(460))

        let phases = subject.events.value.filter { $0.key.role == .space }.map(\.phase)
        XCTAssertEqual(phases, [.pressed, .released])
    }

    func testCancellingSpaceStopsPendingRepeat() async throws {
        let subject = try makeSubject()

        subject.interaction.sendActions(for: .touchDown)
        subject.interaction.sendActions(for: .touchCancel)
        try await Task.sleep(for: .milliseconds(460))

        let phases = subject.events.value.filter { $0.key.role == .space }.map(\.phase)
        XCTAssertEqual(phases, [.pressed, .cancelled])
    }

    private final class EventBox {
        var value: [KeyboardKeyEvent] = []
    }

    private final class Subject {
        let surface: KeyboardSurfaceView
        let interaction: UIControl
        let events: EventBox

        init(surface: KeyboardSurfaceView, interaction: UIControl, events: EventBox) {
            self.surface = surface
            self.interaction = interaction
            self.events = events
        }
    }

    private func makeSubject() throws -> Subject {
        var presentation = KeyboardPresentation()
        presentation.layout = StandardKeyboardLayouts.letters(.vni)
        presentation.isHapticFeedbackEnabled = false
        presentation.showsKeyPreviews = false
        let surface = KeyboardSurfaceView(presentation: presentation)
        let events = EventBox()
        surface.onKeyEvent = { events.value.append($0) }

        let spaceKey = try XCTUnwrap(
            accessibleControls(in: surface).first {
                $0.accessibilityLabel?.hasPrefix("Dấu cách") == true
            }
        )
        let interaction = try XCTUnwrap(
            spaceKey.subviews.compactMap { $0 as? UIControl }.first
        )

        return Subject(surface: surface, interaction: interaction, events: events)
    }
}

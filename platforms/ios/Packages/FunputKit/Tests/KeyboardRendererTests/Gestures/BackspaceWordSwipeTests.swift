#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import KeyboardLayout
import Testing

@MainActor
struct BackspaceWordSwipeTests {
    private static let backspaceKey = KeySpec(id: "backspace", label: "", role: .backspace)

    @Test("Rubbing left one step deletes one word")
    func oneStepDeletesOneWord() {
        let subject = subject()
        subject.begin()
        subject.move(to: 75)

        #expect(subject.claims == [.wordDelete])
        #expect(subject.phases == [.pressed, .deletedWord])
    }

    @Test("Rubbing further deletes further words")
    func ratchetsPerStep() {
        let subject = subject()
        subject.begin()
        subject.move(to: 75)
        subject.move(to: 35)
        subject.move(to: 20)

        #expect(subject.phases == [.pressed, .deletedWord, .deletedWord])
    }

    @Test("Rubbing right does nothing")
    func rightwardIsInert() {
        let subject = subject()
        subject.begin()
        subject.move(to: 220)

        #expect(subject.claims.isEmpty)
        #expect(subject.phases == [.pressed])
    }

    @Test("A claimed rub too short to delete a word still deletes one character")
    func shortRubDegradesToBackspace() {
        let subject = subject()
        subject.begin()
        subject.move(to: 100)
        subject.controller.endTouch(token: 1)

        #expect(subject.phases == [.pressed, .repeated, .cancelled])
    }

    @Test("A claimed rub that deleted words commits no extra backspace")
    func longRubEndsClean() {
        let subject = subject()
        subject.begin()
        subject.move(to: 75)
        subject.controller.endTouch(token: 1)

        #expect(subject.phases == [.pressed, .deletedWord, .cancelled])
    }

    @Test("Once Backspace has repeated, rubbing left keeps repeating characters")
    func repeatWinsOverRatchet() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: 0.4)
        subject.move(to: 75)

        #expect(subject.claims == [.repeatKey])
        #expect(subject.phases == [.pressed, .repeated])
    }

    @Test("With smart gestures off a leftward drag is not a word delete")
    func disabledKeepsOldBehaviour() {
        let subject = subject()
        subject.smartGestures = false
        subject.begin()
        subject.move(to: 75)

        #expect(subject.claims.isEmpty)
        #expect(subject.phases == [.pressed])
    }

    private func subject() -> GestureTestSubject {
        GestureTestSubject(key: Self.backspaceKey)
    }
}
#endif

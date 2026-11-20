#if canImport(UIKit)
import FunputShared
import KeyboardLayout
import Testing
@testable import KeyboardRenderer

/// The Taptic Engine and the audio server are not part of the keyboard's first
/// frame: they are readied once it is on screen and let go when it leaves.
@MainActor
struct KeyboardFeedbackPreparationTests {
    private static let key = KeySpec(id: "a", label: "a", role: .character)

    @Test("Creating the controller and changing the presentation wake nothing")
    func presentationChangeWakesNothing() {
        let subject = GestureTestSubject(key: Self.key)
        subject.soundStyle = .wood
        subject.hapticFeedback = true
        subject.controller.updateFeedback(subject.presentation)
        #expect(subject.haptics.prepares == 0)
        #expect(subject.sounds.prepared.isEmpty)
    }

    @Test("Preparing readies haptics and the chosen sound")
    func preparingReadiesBoth() {
        let subject = GestureTestSubject(key: Self.key)
        subject.soundStyle = .wood
        subject.controller.prepareFeedback(subject.presentation)
        #expect(subject.haptics.prepares == 1)
        #expect(subject.sounds.prepared == [.wood])
    }

    @Test("Preparing with sound off loads no sound")
    func preparingWithSoundOff() {
        let subject = GestureTestSubject(key: Self.key)
        subject.controller.prepareFeedback(subject.presentation)
        #expect(subject.haptics.prepares == 1)
        #expect(subject.sounds.prepared.isEmpty)
    }

    @Test("Releasing lets go of the loaded sound")
    func releasingLetsGo() {
        let subject = GestureTestSubject(key: Self.key)
        subject.soundStyle = .wood
        subject.controller.prepareFeedback(subject.presentation)
        subject.controller.releaseFeedback()
        #expect(subject.sounds.releases == 1)
    }
}
#endif

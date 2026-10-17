#if canImport(UIKit)
import FunputShared
import KeyboardLayout
import Testing
@testable import KeyboardRenderer

@MainActor
struct KeyboardHapticFeedbackTests {
    @Test("Every key uses the selected strength", arguments: HapticFeedbackStrength.allCases)
    func uniformStrength(strength: HapticFeedbackStrength) {
        let roles: [KeyRole] = [.character, .vniModifier, .punctuation, .space, .shift, .backspace, .enter]
        for role in roles {
            let subject = GestureTestSubject(key: KeySpec(id: "key", label: "a", role: role))
            subject.hapticFeedback = true
            subject.hapticStrength = strength
            subject.begin()
            #expect(subject.haptics.performed == [strength])
        }
    }

    @Test("Placeholder keys do not produce feedback")
    func placeholderIsSilent() {
        let subject = GestureTestSubject(key: KeySpec(id: "empty", label: "", role: .placeholder))
        subject.hapticFeedback = true
        subject.begin()
        #expect(subject.haptics.performed.isEmpty)
    }

    @Test("Off also silences repeated backspaces")
    func repeatIsSilent() {
        let subject = GestureTestSubject(key: KeySpec(id: "delete", label: "", role: .backspace))
        subject.begin()
        subject.scheduler.fire(after: KeyboardSurfaceInteractionController.smartBackspaceRepeatDelay)
        #expect(subject.haptics.performed.isEmpty)
        #expect(subject.haptics.ticks == 0)
    }

    @Test("Suggestions read the current strength and enabled flag")
    func suggestionFeedback() {
        let subject = GestureTestSubject(key: KeySpec(id: "a", label: "a", role: .character))
        subject.hapticFeedback = true
        subject.hapticStrength = .medium
        subject.controller.performSuggestionFeedback(presentation: subject.presentation)
        subject.hapticFeedback = false
        subject.controller.performSuggestionFeedback(presentation: subject.presentation)
        #expect(subject.haptics.performed == [.medium])
    }
}
#endif

#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// After an undo the trigger is kept as typed once, for the next word only.
@MainActor
struct KeyboardShortcutRefusalTests {
    @Test("The refused trigger stays as typed once, then expands again")
    func keptOnce() {
        let fixture = ShortcutUndoFixture()
        fixture.type("vn ")
        fixture.backspace()
        fixture.type("vn ")
        #expect(fixture.text == "vn vn ")
        #expect(fixture.preview == nil)

        fixture.type("vn ")
        #expect(fixture.text == "vn vn Việt Nam ")
    }

    @Test("Any word that ends spends the refusal, so it cannot outlive the sentence")
    func spentByAnotherWord() {
        let fixture = ShortcutUndoFixture(shortcuts: ["vn": "Việt Nam", "kg": "không"])
        fixture.type("vn ")
        fixture.backspace()
        fixture.type("kg ")
        #expect(fixture.text == "vn không ")

        fixture.type("vn ")
        #expect(fixture.text == "vn không Việt Nam ")
    }

    @Test("Erasing the undone word gives the trigger its effect back")
    func erasingDismisses() {
        for language in KeyboardLanguage.allCases {
            let fixture = ShortcutUndoFixture(language: language)
            fixture.type("vn ")
            fixture.backspace()
            #expect(fixture.text == "vn ")
            fixture.backspace(3)
            #expect(fixture.text == "")

            fixture.type("vn ")
            #expect(fixture.text == "Việt Nam ", "\(language)")
        }
    }

    @Test("A different trigger still expands right after one is refused")
    func otherTriggerExpands() {
        let fixture = ShortcutUndoFixture(shortcuts: ["vn": "Việt Nam", "kg": "không"])
        fixture.type("vn ")
        fixture.backspace()
        fixture.type("kg ")
        #expect(fixture.text == "vn không ")
    }

    @Test("A refused word is not repaired as a typo either")
    func notRepaired() {
        let fixture = ShortcutUndoFixture(shortcuts: ["dduwowfnh": "đường"])
        fixture.type("dduwowfnh ")
        #expect(fixture.text == "đường ")
        fixture.backspace()
        fixture.type("dduwowfnh ")
        #expect(fixture.text == "dduwowfnh dduwowfnh ")
    }
}
#endif

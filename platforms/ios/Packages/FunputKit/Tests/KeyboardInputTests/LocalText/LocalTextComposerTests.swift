#if os(iOS) && canImport(FunputCore)
import KeyboardInput
import KeyboardLayout
import Testing

/// A panel's own field (the emoji search) composing with the document's rules.
@MainActor
struct LocalTextComposerTests {
    @Test("Telex composes tones, horns and đ")
    func telex() {
        let field = makeField(.telex)
        typeLocal("cuwowif ddor", into: field)
        #expect(field.text == "cười đỏ")
    }

    @Test("VNI composes from the modifier digits")
    func vni() {
        let field = makeField(.vni)
        typeLocal("cuo7i2 d9o3", into: field)
        #expect(field.text == "cười đỏ")
    }

    @Test("English passes every key through")
    func english() {
        let coordinator = KeyboardInputCoordinator(inputMethod: .telex)
        coordinator.toggleLanguage()
        let field = LocalTextComposer()
        coordinator.configure(field)
        typeLocal("as", into: field)
        #expect(field.text == "as")
    }

    @Test("Backspace steps back inside the composition")
    func backspaceInsideWord() {
        let field = makeField(.telex)
        typeLocal("as", into: field)
        field.apply(.deleteBackward)
        #expect(field.text.isEmpty)
        typeLocal("es", into: field)
        #expect(field.text == "é")
    }

    @Test("Backspace over the space re-opens the word for a new tone")
    func retonesAfterBackspace() {
        let field = makeField(.telex)
        typeLocal("chaof ", into: field)
        #expect(field.text == "chào ")
        field.apply(.deleteBackward)
        typeLocal("s", into: field)
        #expect(field.text == "cháo")
    }

    @Test("Leading and repeated spaces are dropped")
    func spacing() {
        let field = makeField(.telex)
        typeLocal("  as  as", into: field)
        #expect(field.text == "á á")
    }

    @Test("Reset leaves no composition behind")
    func reset() {
        let field = makeField(.telex)
        typeLocal("a", into: field)
        field.reset()
        typeLocal("s", into: field)
        #expect(field.text == "s")
    }

    private func makeField(_ method: KeyboardInputMethod) -> LocalTextComposer {
        let field = LocalTextComposer()
        KeyboardInputCoordinator(inputMethod: method).configure(field)
        return field
    }
}

@MainActor
func typeLocal(_ keys: String, into field: LocalTextComposer) {
    for character in keys {
        field.apply(character == " " ? .space : .text(String(character)))
    }
}
#endif

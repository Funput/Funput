#if os(iOS) && canImport(FunputCore)
import FunputEngine
import Testing

/// `FunputComposer.configure` marshals ``FunputCompositionOptions`` into the C ABI's
/// `FunputConfig` field by field. A swapped field would still compile, so each option
/// is asserted through observable composition behavior rather than by inspection.
@MainActor
struct FunputConfigureTests {
    private func options(
        inputMethod: FunputInputMethod = .telex,
        toneStyle: FunputToneStyle = .traditional,
        smartRestore: Bool = true,
        eagerRestore: Bool = true,
        spellCheck: Bool = false,
        autoCapitalize: Bool = false
    ) -> FunputCompositionOptions {
        FunputCompositionOptions(
            inputMethod: inputMethod,
            toneStyle: toneStyle,
            smartRestore: smartRestore,
            eagerRestore: eagerRestore,
            spellCheck: spellCheck,
            autoCapitalize: autoCapitalize
        )
    }

    @Test("Configure applies the input method")
    func inputMethod() {
        let composer = FunputComposer()
        composer.configure(options(inputMethod: .vni))
        composer.process("a")
        composer.process("1") // VNI: digit is the tone key
        #expect(composer.buffer() == "á")
    }

    @Test("Configure applies the tone style")
    func toneStyle() {
        // `hoaf` places the tone traditionally on `o`, modern on `a`.
        let traditional = FunputComposer()
        traditional.configure(options(toneStyle: .traditional))
        let modern = FunputComposer()
        modern.configure(options(toneStyle: .modern))

        for scalar in "hoaf".unicodeScalars {
            traditional.process(scalar)
            modern.process(scalar)
        }
        #expect(traditional.buffer() == "hòa")
        #expect(modern.buffer() == "hoà")
    }

    @Test("Configure applies spell-check independently of the other toggles")
    func spellCheck() {
        // `tetf` is not a real syllable: with spell-check the huyền key stays literal.
        let checked = FunputComposer()
        checked.configure(options(smartRestore: false, spellCheck: true))
        let unchecked = FunputComposer()
        unchecked.configure(options(smartRestore: false, spellCheck: false))

        for scalar in "tetf".unicodeScalars {
            checked.process(scalar)
            unchecked.process(scalar)
        }
        #expect(checked.buffer() == "tetf")
        #expect(unchecked.buffer() == "tèt")
    }

    // No auto-capitalize test here any more. It only ever exercised
    // `armCapitalization()`, which iOS never called and which is now gone: case on
    // this platform comes from the Shift state, covered by
    // KeyboardCapitalizationOwnershipTests. The engine's own sentence tracker is
    // covered where it is used from, in crates/funput-engine/tests/engine_api.rs.

    @Test("Configure applies eager restore")
    func eagerRestore() {
        // `text` dead-ends as Vietnamese: eager restore flips it back immediately,
        // while without it the composed form is kept until a word boundary.
        let eager = FunputComposer()
        eager.configure(options(eagerRestore: true))
        let lazy = FunputComposer()
        lazy.configure(options(eagerRestore: false))

        for scalar in "text".unicodeScalars {
            eager.process(scalar)
            lazy.process(scalar)
        }
        #expect(eager.buffer() == "text")
        #expect(lazy.buffer() == "tẽt")
    }
}
#endif

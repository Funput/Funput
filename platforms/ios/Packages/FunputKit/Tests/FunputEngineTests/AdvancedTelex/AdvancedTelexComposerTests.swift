#if os(iOS) && canImport(FunputCore)
import FunputEngine
import Testing

@MainActor
struct AdvancedTelexComposerTests {
    @Test("Input method wire values remain stable")
    func wireValues() {
        #expect(FunputInputMethod.telex.rawValue == 0)
        #expect(FunputInputMethod.vni.rawValue == 1)
        #expect(FunputInputMethod.telexAdvanced.rawValue == 2)
    }

    @Test("Advanced Telex shortcuts compose through the FFI wrapper")
    func shortcuts() {
        let cases = [
            ("w", "ư"), ("wf", "ừ"), ("t[", "tư"), ("m]", "mơ"),
            ("tr[]ngf", "trường"), ("ng[]if", "người"), ("ww", "w"),
        ]
        for (keys, expected) in cases {
            #expect(compose(keys) == expected, "Failed input: \(keys)")
        }
    }

    @Test("Advanced Telex preserves capitalization and tone style")
    func options() {
        #expect(compose("Wf") == "Ừ")
        #expect(compose("hoaf", toneStyle: .traditional) == "hòa")
        #expect(compose("hoaf", toneStyle: .modern) == "hoà")
    }

    @Test("Changing method clears an active composition")
    func methodChangeClearsComposition() {
        let composer = configuredComposer()
        composer.process("w")
        #expect(composer.buffer() == "ư")
        composer.setInputMethod(.telex)
        #expect(composer.buffer().isEmpty)
    }

    private func compose(
        _ keys: String,
        toneStyle: FunputToneStyle = .traditional
    ) -> String {
        let composer = configuredComposer(toneStyle: toneStyle)
        for scalar in keys.unicodeScalars {
            composer.process(scalar)
        }
        return composer.buffer()
    }

    private func configuredComposer(
        toneStyle: FunputToneStyle = .traditional
    ) -> FunputComposer {
        let composer = FunputComposer()
        composer.configure(
            FunputCompositionOptions(
                inputMethod: .telexAdvanced,
                toneStyle: toneStyle,
                smartRestore: true,
                eagerRestore: true,
                spellCheck: false,
                autoCapitalize: false
            )
        )
        return composer
    }
}
#endif

import FunputEngine
import Testing

struct FunputExtraOnsetOptionsTests {
    @Test("Existing callers keep extra onsets disabled")
    func defaultOptions() {
        let options = FunputCompositionOptions(
            inputMethod: .telex, toneStyle: .modern,
            smartRestore: true, eagerRestore: true, spellCheck: false
        )
        #expect(options.extraOnsets.isEmpty)
    }
}

#if os(iOS) && canImport(FunputCore)
import FunputCore

@MainActor
struct FunputExtraOnsetsTests {
    @Test("Swift onset bits match the C ABI")
    func abiBits() {
        #expect(FunputExtraOnsets.f.rawValue == UInt8(ONSET_F))
        #expect(FunputExtraOnsets.j.rawValue == UInt8(ONSET_J))
        #expect(FunputExtraOnsets.w.rawValue == UInt8(ONSET_W))
        #expect(FunputExtraOnsets.z.rawValue == UInt8(ONSET_Z))
    }

    @Test("Only selected initial consonants compose Vietnamese")
    func selectiveOnsets() {
        let composer = FunputComposer()
        composer.configure(options([.z]))
        #expect(compose("zoo ", with: composer) == "zô ")
        #expect(compose("fair ", with: composer) == "fair ")
        composer.configure(options([.z, .f]))
        #expect(compose("fair ", with: composer) == "fải ")
        #expect(compose("food ", with: composer) == "food ")
        #expect(compose("fast ", with: composer) == "fát ")
        #expect(compose("fasst ", with: composer) == "fast ")
    }

    @Test("Each extended consonant can be enabled independently")
    func individualLetters() {
        let cases: [(FunputExtraOnsets, String, String)] = [
            (.z, "zoo ", "zô "), (.f, "fair ", "fải "),
            (.w, "was ", "wá "), (.j, "jowf ", "jờ "),
        ]
        for (letter, keys, expected) in cases {
            let composer = FunputComposer()
            composer.configure(options(letter))
            #expect(compose(keys, with: composer) == expected)
        }
    }

    @Test("Reconfiguring can disable and retain the chosen onsets")
    func reconfiguration() {
        let composer = FunputComposer()
        composer.configure(options([.z]))
        #expect(compose("zoo ", with: composer) == "zô ")
        composer.configure(options([.z]))
        #expect(compose("zoo ", with: composer) == "zô ")
        composer.configure(options([]))
        #expect(compose("zoo ", with: composer) == "zoo ")
    }

    @Test("VNI and advanced Telex retain their onset conventions")
    func inputMethods() {
        let composer = FunputComposer()
        let all: FunputExtraOnsets = [.z, .f, .w, .j]
        composer.configure(options(all, method: .vni))
        #expect(compose("zo6 jo72 ", with: composer) == "zô jờ ")
        composer.configure(options(all, method: .telexAdvanced))
        #expect(compose("wa wwas ", with: composer) == "ưa wá ")
    }

    private func options(
        _ letters: FunputExtraOnsets, method: FunputInputMethod = .telex
    ) -> FunputCompositionOptions {
        FunputCompositionOptions(
            inputMethod: method, toneStyle: .modern,
            smartRestore: true, eagerRestore: true, spellCheck: false,
            extraOnsets: letters
        )
    }

    private func compose(_ keys: String, with composer: FunputComposer) -> String {
        composer.clear()
        var text = ""
        for scalar in keys.unicodeScalars {
            let result = composer.process(scalar)
            if result.action == .none { text.unicodeScalars.append(scalar) }
            else {
                text.unicodeScalars.removeLast(result.deleteCount)
                text.append(result.text)
            }
        }
        return text
    }
}
#endif

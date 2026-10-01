#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// A dictionary that knows exactly the words a test names.
@MainActor
private final class TestDictionary: CorrectionDictionary {
    var words: Set<String> = []
    func recognizes(_ word: String) -> Bool { words.contains(word) }
    func keep(_ word: String) { words.insert(word) }
}

@MainActor
struct KeyboardCorrectionTests {
    private func coordinator(
        dictionary: TestDictionary?,
        allowsAutocorrect: Bool = true
    ) -> KeyboardInputCoordinator {
        let coordinator = KeyboardInputCoordinator()
        coordinator.apply(
            FunputConfiguration(inputMethod: .telex, typoCorrection: true)
        )
        coordinator.correctionDictionary = dictionary
        coordinator.allowsAutocorrect = allowsAutocorrect
        return coordinator
    }

    /// Type `keys`, reporting the key at `slip` as having drifted towards a neighbour.
    private func type(
        _ keys: String,
        slip: (Int, Unicode.Scalar)?,
        with coordinator: KeyboardInputCoordinator,
        into document: TestKeyboardWriter
    ) {
        for (index, character) in keys.enumerated() {
            let key = character == " "
                ? testKey(.space)
                : testKey(.character, label: String(character))
            var touch: KeyboardTouchEvidence?
            if let scalar = String(character).unicodeScalars.first, character != " " {
                var evidence = KeyboardTouchEvidence(typed: scalar, typedDistance: 0.35)
                if let slip, slip.0 == index {
                    evidence = KeyboardTouchEvidence(
                        typed: scalar,
                        typedDistance: 0.35,
                        first: .init(scalar: slip.1, distance: 0.15)
                    )
                }
                touch = evidence
            }
            coordinator.handle(key, touch: touch, writer: document)
        }
    }

    @Test("A repaired word and the space that ended it ride one transaction")
    func oneTransaction() {
        // Two transactions would close the echo epoch between the edits, expose the
        // half-written document to `textDidChange`, and let the suggestion tracker
        // learn the typo before the repair replaced it.
        let dictionary = TestDictionary()
        let coordinator = coordinator(dictionary: dictionary)
        let document = TestKeyboardWriter()

        type("dduwowfnh ", slip: (8, "g"), with: coordinator, into: document)

        #expect(document.text == "đường ")
        #expect(document.transactions.count == 10, "one per key, none extra")
    }

    @Test("A word the dictionary knows is left exactly as it was typed")
    func vetoed() {
        // `text` ends as raw keys the same way a mistyped Vietnamese word does, and
        // `r` sits beside `t`, so the engine can reach `tẻ` from it. Only the
        // dictionary can tell those apart.
        let dictionary = TestDictionary()
        dictionary.words = ["text"]
        let coordinator = coordinator(dictionary: dictionary)
        let document = TestKeyboardWriter()

        type("text ", slip: (3, "r"), with: coordinator, into: document)

        #expect(document.text == "text ")
        #expect(coordinator.correctionDeclines.recognized == 1)
    }

    @Test("Without a dictionary nothing is ever corrected")
    func noDictionary() {
        let coordinator = coordinator(dictionary: nil)
        let document = TestKeyboardWriter()

        type("dduwowfnh ", slip: (8, "g"), with: coordinator, into: document)

        #expect(document.text == "dduwowfnh ")
    }

    @Test("A field that asked for no autocorrect gets none")
    func autocorrectOff() {
        let coordinator = coordinator(dictionary: TestDictionary(), allowsAutocorrect: false)
        let document = TestKeyboardWriter()

        type("dduwowfnh ", slip: (8, "g"), with: coordinator, into: document)

        #expect(document.text == "dduwowfnh ")
        #expect(coordinator.correctionDeclines.fieldRefused == 1)
        #expect(coordinator.correctionDeclines.recognized == 0)
    }

    @Test("Touch spread is measured whether or not anything is corrected")
    func spread() {
        // The number that decides whether the feature is worth having at all.
        let coordinator = coordinator(dictionary: nil)
        let document = TestKeyboardWriter()

        type("dduwowfnh ", slip: nil, with: coordinator, into: document)

        #expect(coordinator.touchSpread.count == 9)
        #expect(abs(coordinator.touchSpread.mean - 0.35) < 0.001)
        // Distances from a centre are not the per-axis spread the model is written
        // in: touches scattered by σ each way land about 1.25σ out.
        #expect(abs(coordinator.touchSpread.perAxisSigma - 0.35 / 2.0.squareRoot()) < 0.001)
    }

    @Test("A session's touches are never counted twice")
    func reportedOnce() {
        // `viewWillDisappear` arrives more than once for a keyboard extension.
        let coordinator = coordinator(dictionary: nil)
        let document = TestKeyboardWriter()
        type("dduwowfnh ", slip: nil, with: coordinator, into: document)

        coordinator.resetTouchSpread()

        #expect(coordinator.touchSpread.count == 0)
    }
}
#endif

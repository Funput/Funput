#if os(iOS) && canImport(FunputCore)
import FunputShared
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// A correction the engine would not decide at the boundary stays on the bar after
/// Space, over the committed word, until the next key. Real engine throughout:
/// `nhad` with the finger exactly between `f` and `s` is `nhà` or `nhá` on touch
/// alone, and a dictionary with no counts cannot break the tie.
@MainActor
struct KeyboardCorrectionParkedTests {
    let coordinator = KeyboardInputCoordinator()
    let document = TestKeyboardWriter()
    let dictionary = WeighingDictionary()

    init() {
        coordinator.apply(FunputConfiguration(inputMethod: .telex, typoCorrection: true))
        coordinator.correctionDictionary = dictionary
    }

    @Test("Space leaves the word, and the bar keeps both repairs over it")
    func parkedAfterSpace() {
        typeUndecided()

        #expect(document.text == "nhad ")
        #expect(coordinator.correctionPreview == .parked(typed: "nhad", candidates: ["nhà", "nhá"]))
    }

    @Test("Tapping a parked repair replaces the word, keeps the space, and arms the undo")
    func acceptParked() {
        typeUndecided()
        let before = document.transactions.count

        coordinator.acceptCorrection("nhà", writer: document)

        #expect(document.text == "nhà ")
        #expect(document.transactions.count == before + 1, "one transaction")
        #expect(coordinator.correctionPreview == .applied(undoText: "nhad"))
        coordinator.handle(testKey(.backspace), writer: document)
        #expect(document.text == "nhad ")
    }

    @Test("Tapping the typed word keeps it, and it is never offered again")
    func refuseParked() {
        typeUndecided()

        coordinator.refuseCorrection(typed: "nhad", writer: document)

        #expect(document.text == "nhad ")
        #expect(coordinator.correctionPreview == nil)
        #expect(dictionary.kept == ["nhad"])
        #expect(coordinator.correctionDeclines.refused == 1)
        typeUndecided()
        #expect(coordinator.correctionPreview == nil)
    }

    @Test("The next key lets the parked repair go")
    func nextKeyFlushes() {
        typeUndecided()

        type("x")

        #expect(document.text == "nhad x")
        #expect(coordinator.correctionPreview == nil)
    }

    @Test("Backspace right after Space drops it too")
    func backspaceDrops() {
        typeUndecided()

        coordinator.handle(testKey(.backspace), writer: document)

        #expect(document.text == "nhad")
        #expect(coordinator.correctionPreview == nil)
    }

    @Test("A word the dictionary knows is not parked, nor is one in a field without autocorrect")
    func notParked() {
        dictionary.words = ["nhad"]
        typeUndecided()
        #expect(coordinator.correctionPreview == nil)

        dictionary.words = []
        coordinator.allowsAutocorrect = false
        typeUndecided()
        #expect(coordinator.correctionPreview == nil)
        #expect(document.text == "nhad nhad ")
    }

    /// `nhad` and a space, with the last key landing exactly between `f` and `s`.
    private func typeUndecided() {
        type("nha")
        let between = KeyboardTouchEvidence(
            typed: "d", typedDistance: 0.3,
            first: .init(scalar: "f", distance: 0.3), second: .init(scalar: "s", distance: 0.3)
        )
        coordinator.handle(testKey(.character, label: "d"), touch: between, writer: document)
        coordinator.handle(testKey(.space), writer: document)
    }

    /// Letters typed square on their keys.
    private func type(_ keys: String) {
        for character in keys {
            let scalar = String(character).unicodeScalars.first!
            coordinator.handle(
                testKey(.character, label: String(character)),
                touch: KeyboardTouchEvidence(typed: scalar, typedDistance: 0.05),
                writer: document
            )
        }
    }
}
#endif

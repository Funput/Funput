#if os(iOS) && canImport(FunputCore)
import KeyboardLayout
import Testing

@testable import KeyboardInput

/// What the bar is told about the word under the caret, and what its taps do.
@MainActor
struct KeyboardCorrectionPreviewTests {
    @Test("A dead-ended word with a repair in reach is offered, weighed by the dictionary")
    func offered() {
        let fixture = PreviewFixture()

        fixture.type("dduwowfnh")

        #expect(fixture.coordinator.correctionPreview == .offered(typed: "dduwowfnh", candidates: ["đường"]))
        #expect(fixture.previewer.rankedUses.last == [7], "the prior reaches the ranking")
    }

    @Test("The real engine offers the same repair the boundary would apply")
    func realEngine() {
        let fixture = PreviewFixture()
        fixture.coordinator.correctionPreviewer = fixture.coordinator.composer

        fixture.type("dduwowfnh")

        #expect(fixture.coordinator.correctionPreview == .offered(typed: "dduwowfnh", candidates: ["đường"]))
        fixture.type(" ", slip: nil)
        #expect(fixture.document.text == "đường ")
    }

    @Test("A live word is left alone")
    func liveWord() {
        let fixture = PreviewFixture()

        fixture.type("chaof", slip: nil)

        #expect(fixture.coordinator.correctionPreview == nil)
    }

    @Test("A word the dictionary knows is never previewed")
    func knownWord() {
        let fixture = PreviewFixture()
        fixture.dictionary.words = ["dduwowfnh"]

        fixture.type("dduwowfnh")

        #expect(fixture.coordinator.correctionPreview == nil)
        #expect(fixture.previewer.rankedUses.isEmpty, "the engine was not even asked")
    }

    @Test("A field that asked for no autocorrect gets no preview either")
    func fieldRefused() {
        let fixture = PreviewFixture()
        fixture.coordinator.allowsAutocorrect = false

        fixture.type("dduwowfnh")

        #expect(fixture.coordinator.correctionPreview == nil)
    }

    @Test("Tapping the repair commits it with its space, in one transaction")
    func accept() {
        let fixture = PreviewFixture()
        fixture.type("dduwowfnh")

        fixture.coordinator.acceptCorrection("đường", writer: fixture.document)

        #expect(fixture.document.text == "đường ")
        #expect(fixture.document.transactions.count == 10, "nine keys and one tap")
        #expect(fixture.coordinator.correctionPreview == .applied(undoText: "dduwowfnh"))
    }

    @Test("Tapping the typed word keeps it, now and from then on")
    func refuse() {
        let fixture = PreviewFixture()
        fixture.type("dduwowfnh")

        fixture.coordinator.refuseCorrection(typed: "dduwowfnh", writer: fixture.document)
        fixture.type(" ", slip: nil)

        #expect(fixture.document.text == "dduwowfnh ")
        #expect(fixture.dictionary.kept == ["dduwowfnh"])
        #expect(fixture.previewer.refusals == 1)
        #expect(fixture.coordinator.correctionDeclines.refused == 1)
        #expect(fixture.coordinator.correctionPreview == nil)
    }

    @Test("The undo chip does exactly what Backspace would")
    func undoFromBar() {
        let fromBar = PreviewFixture()
        let fromKey = PreviewFixture()
        fromBar.type("dduwowfnh ")
        fromKey.type("dduwowfnh ")
        #expect(fromBar.coordinator.correctionPreview == .applied(undoText: "dduwowfnh"))

        fromBar.coordinator.undoCorrectionFromBar(writer: fromBar.document)
        fromKey.coordinator.handle(testKey(.backspace), writer: fromKey.document)

        #expect(fromBar.document.text == "dduwowfnh ")
        #expect(fromBar.document.text == fromKey.document.text)
        #expect(fromBar.dictionary.kept == fromKey.dictionary.kept)
        #expect(fromBar.coordinator.correctionPreview == nil)
    }

    @Test("The boundary weighs candidates with the dictionary's counts")
    func usesReachTheBoundary() {
        let fixture = PreviewFixture()

        fixture.type("dduwowfnh ")

        #expect(fixture.document.text == "đường ")
        #expect(fixture.dictionary.weighed.contains("đường"))
    }

    @Test("Two repairs too close to call are both offered")
    func ambiguous() {
        let fixture = PreviewFixture()
        fixture.previewer.repairs["dduwowfnh"] = ["đường", "đượng"]
        fixture.previewer.verdict = .ambiguous

        fixture.type("dduwowfnh")

        #expect(
            fixture.coordinator.correctionPreview
                == .offered(typed: "dduwowfnh", candidates: ["đường", "đượng"])
        )
    }

    @Test("Backspace recomputes the preview for the shorter word")
    func backspace() {
        let fixture = PreviewFixture()
        fixture.type("dduwowfnh")

        fixture.coordinator.handle(testKey(.backspace), writer: fixture.document)

        #expect(fixture.coordinator.correctionPreview == nil)
    }
}
#endif

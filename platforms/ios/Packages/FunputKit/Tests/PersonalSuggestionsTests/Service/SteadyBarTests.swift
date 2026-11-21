import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import Testing

/// The bar between two answers: what it showed stays up, and takes no tap.
@MainActor
@Suite("Steady suggestion bar")
struct SteadyBarTests {
    @Test("A new prefix keeps the words on the bar until its own answer lands")
    func keepsWordsWhileAsking() async throws {
        let probe = ServiceProbe()
        probe.type("ba")
        await probe.answer("bạn", "bán")
        let draws = probe.drawn.count

        probe.type("ban")

        #expect(probe.drawn.count == draws, "nothing is redrawn, least of all an empty bar")
        #expect(probe.shown == ["bạn", "bán"])
        #expect(probe.queries.last?.prefix == "ban")
    }

    @Test("A kept word takes no tap: it answered an older prefix")
    func keptWordsRefuseTaps() async throws {
        let probe = ServiceProbe()
        probe.type("ba")
        await probe.answer("bạn")
        let kept = try #require(probe.drawn.last?.first)
        #expect(probe.service.acceptance(for: kept)?.0 == "ba")

        probe.type("bac")

        #expect(probe.service.acceptance(for: kept) == nil)
    }

    @Test("The same words answering the new prefix reach the bar and commit over it")
    func sameAnswerIsTappableAgain() async throws {
        let probe = ServiceProbe()
        probe.type("ba")
        await probe.answer("bạn")
        probe.type("ban")
        await probe.answer("bạn")

        let fresh = try #require(probe.drawn.last?.first)
        let acceptance = try #require(probe.service.acceptance(for: fresh))
        #expect(acceptance == ("ban", "bạn"))
    }

    @Test("An empty answer clears the bar")
    func emptyAnswerClears() async {
        let probe = ServiceProbe()
        probe.type("ba")
        await probe.answer("bạn")
        probe.type("bax")
        await probe.answer()

        #expect(probe.shown.isEmpty)
    }

    @Test("With nothing to ask, the bar clears at once")
    func nothingAskedClearsAtOnce() async {
        let probe = ServiceProbe()
        probe.type("", after: "cảm")
        await probe.answer("ơn")
        #expect(probe.shown == ["ơn"])

        // One letter is neither a prefix worth completing nor a word boundary.
        probe.type("c", after: "cảm")

        #expect(probe.shown.isEmpty)
        #expect(probe.queries.count == 1)
    }

    @Test("A correction is drawn at once, beside the words still kept")
    func correctionJoinsKeptWords() async throws {
        let probe = ServiceProbe()
        probe.type("đươ")
        await probe.answer("được")

        probe.service.update(
            .init(prefix: "đườnh", completedToken: nil),
            canQuery: true,
            correction: .offered(typed: "đườnh", candidates: ["đường"])
        )

        #expect(probe.shown == ["đườnh", "đường", "được"])
        let repair = try #require(probe.drawn.last?[1])
        #expect(probe.service.correctionAction(for: repair) == .accept(text: "đường"))
    }
}

import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import PersonalSuggestions
import Testing

/// What the bar shows is the store's word in the user's case. Learned words arrive
/// lowercase and dictionary words arrive with their own shape, so every capital here is
/// one this layer put there.
@MainActor
struct SuggestionCasingTests {
    @Test(
        "The prefix sets the case, and Shift overrules it",
        arguments: [
            // A lowercase prefix leaves the word alone, which is how `iPhone` survives.
            (prefix: "ip", shift: ShiftState.lowercase, stored: "iPhone", shown: "iPhone"),
            (prefix: "IP", shift: .lowercase, stored: "iphone", shown: "IPHONE"),
            (prefix: "Vi", shift: .lowercase, stored: "việt", shown: "Việt"),
            // One capital letter is how a sentence starts, not how it shouts.
            (prefix: "V1", shift: .lowercase, stored: "việt", shown: "Việt"),
            // Shift over an already typed lowercase prefix is still a request for
            // capitals: accepting rewrites those letters anyway.
            (prefix: "vi", shift: .uppercase, stored: "việt", shown: "Việt"),
            (prefix: "vi", shift: .capsLocked, stored: "việt", shown: "VIỆT"),
        ]
    )
    func casing(_ example: (prefix: String, shift: ShiftState, stored: String, shown: String))
        async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service)
        var delivered: [[String]] = []
        service.onCandidates = { _, values in
            if !values.isEmpty { delivered.append(values.map(\.text)) }
        }

        service.update(
            .init(prefix: example.prefix, completedToken: nil),
            canQuery: true,
            shift: example.shift
        )
        let worker = try #require(factory.workers.first)
        worker.emit([example.stored], for: try #require(worker.events.compactMap(\.query).last))
        await Task.yield()

        #expect(delivered.last == [example.shown])
    }

    @Test("A Shift tap re-cases the words already on the bar, and they still commit")
    func recasingKeepsTheListTappable() async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service)
        var delivered: [[KeyboardSuggestionCandidate]] = []
        service.onCandidates = { _, values in if !values.isEmpty { delivered.append(values) } }

        service.update(.init(prefix: "vi", completedToken: nil), canQuery: true)
        let worker = try #require(factory.workers.first)
        worker.emit(["việt"], for: try #require(worker.events.compactMap(\.query).last))
        await Task.yield()
        #expect(delivered.last?.map(\.text) == ["việt"])

        service.recase(shift: .uppercase)
        await Task.yield()

        // The same word in a new shape, and no second question asked of the engine.
        #expect(delivered.last?.map(\.text) == ["Việt"])
        #expect(worker.events.compactMap(\.query).count == 1)
        // The accept path matches on the candidate it published, so a re-cased list has
        // to stay tappable — otherwise every tap after a Shift silently does nothing.
        let candidate = try #require(delivered.last?.first)
        let acceptance = try #require(service.acceptance(for: candidate))
        #expect(acceptance == ("vi", "Việt"))
    }
}

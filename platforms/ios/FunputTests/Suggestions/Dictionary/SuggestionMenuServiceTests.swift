import KeyboardInput
import KeyboardRenderer
import PersonalDictionary
import PersonalSuggestions
import Testing

/// A long-press edit on the bar reaches the store under the stored spelling, and the
/// bar asks again so the word leaves it at once.
@MainActor
struct SuggestionMenuServiceTests {
    private func shownWord(
        _ stored: String,
        prefix: String
    ) async throws -> (PersonalSuggestionService, SuggestionWorkerSpy, KeyboardSuggestionCandidate) {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service)
        var shown: [KeyboardSuggestionCandidate] = []
        service.onCandidates = { _, values in if !values.isEmpty { shown = values } }
        service.update(.init(prefix: prefix, completedToken: nil), canQuery: true)
        let worker = try #require(factory.workers.first)
        worker.emit([stored], for: try #require(worker.events.compactMap(\.query).last))
        await Task.yield()
        return (service, worker, try #require(shown.first))
    }

    @Test("The edit carries the stored spelling, not the one on screen")
    func editUsesTheStoredWord() async throws {
        let (service, worker, candidate) = try await shownWord("zalo", prefix: "ZA")
        #expect(candidate.text == "ZALO")
        let queries = worker.events.compactMap(\.query).count

        service.edit(candidate, .block)

        #expect(worker.events.contains(.edit(.block, word: "zalo")))
        let requery = try #require(worker.events.compactMap(\.query).last)
        #expect(worker.events.compactMap(\.query).count == queries + 1)
        #expect(requery.generation > candidate.generation)
        #expect(requery.prefix == "ZA")
    }

    @Test("A candidate from a bar that has moved on is ignored")
    func staleCandidateIsIgnored() async throws {
        let (service, worker, candidate) = try await shownWord("chào", prefix: "ch")
        service.update(.init(prefix: "chà", completedToken: nil), canQuery: true)

        service.edit(candidate, .forget)

        #expect(!worker.events.contains { if case .edit = $0 { true } else { false } })
    }
}

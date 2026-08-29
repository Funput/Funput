import Foundation
import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import Testing

@MainActor
struct PersonalSuggestionServiceCreationTests {
    @Test("Configure and flush do not create a worker")
    func configureIsLazy() {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()

        _ = factory.configure(service)
        service.flush()

        #expect(factory.workers.isEmpty)
    }

    @Test("First valid query creates one configured worker")
    func queryCreatesOnce() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        let configuration = factory.configure(service)

        service.update(update(prefix: "kh"), canQuery: true)
        service.update(update(prefix: "kh"), canQuery: true)

        let worker = try #require(factory.workers.first)
        #expect(factory.workers.count == 1)
        #expect(worker.events.first == .configure(configuration))
        #expect(worker.events.compactMap(\.query).map(\.prefix) == ["kh"])
    }

    @Test("Learning configures the worker before the token")
    func learnOrdering() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        let configuration = factory.configure(service)

        service.update(
            .init(prefix: "", completedToken: "chào"),
            canQuery: false
        )

        let worker = try #require(factory.workers.first)
        #expect(worker.events == [.configure(configuration), .learn("chào", context: nil)])
    }

    @Test("Disabled suggestions never create a worker")
    func disabledPath() {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        _ = factory.configure(service, enabled: false)

        service.update(
            .init(prefix: "kh", completedToken: "chào"),
            canQuery: true
        )
        service.flush()

        #expect(factory.workers.isEmpty)
    }

    @Test("The context reaches both the learn and the query")
    func forwardsContext() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        _ = factory.configure(service)

        // "xin" finishes on a space, so it is what the next word follows.
        service.update(.init(prefix: "", completedToken: "xin", context: "xin"), canQuery: true)
        service.update(.init(prefix: "ch", completedToken: nil, context: "xin"), canQuery: true)

        let worker = try #require(factory.workers.first)
        #expect(worker.events.compactMap(\.learned).last?.context == nil)
        #expect(worker.events.compactMap(\.query).last?.context == "xin")

        // The word that follows is learned against the context, not after it.
        service.update(.init(prefix: "", completedToken: "chào", context: "chào"), canQuery: true)
        let learned = try #require(worker.events.compactMap(\.learned).last)
        #expect(learned.token == "chào")
        #expect(learned.context == "xin")
    }

    @Test("A context with no prefix asks for a prediction")
    func predictsAfterASpace() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        _ = factory.configure(service)

        service.update(.init(prefix: "", completedToken: "xin", context: "xin"), canQuery: true)

        let worker = try #require(factory.workers.first)
        let query = try #require(worker.events.compactMap(\.query).last)
        #expect(query.prefix.isEmpty)
        #expect(query.context == "xin")
    }

    @Test("One character is neither a prefix nor a boundary")
    func oneCharacterAsksNothing() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        _ = factory.configure(service)

        service.update(.init(prefix: "", completedToken: "xin", context: "xin"), canQuery: true)
        let worker = try #require(factory.workers.first)
        let afterSpace = worker.events.compactMap(\.query).count

        service.update(.init(prefix: "c", completedToken: nil, context: "xin"), canQuery: true)
        #expect(worker.events.compactMap(\.query).count == afterSpace)
    }

    @Test("A prediction can be accepted, replacing nothing")
    func acceptsAPrediction() async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        var delivered: [[KeyboardSuggestionCandidate]] = []
        service.onCandidates = { _, values in if !values.isEmpty { delivered.append(values) } }
        _ = factory.configure(service)

        service.update(.init(prefix: "", completedToken: "xin", context: "xin"), canQuery: true)
        let worker = try #require(factory.workers.first)
        worker.emit(["chào"], for: try #require(worker.events.compactMap(\.query).last))
        await Task.yield()

        let candidate = try #require(delivered.last?.first)
        let acceptance = try #require(
            service.acceptance(for: candidate),
            "a prediction nobody can tap is no use at all"
        )
        #expect(acceptance.0.isEmpty)
        #expect(acceptance.1 == "chào")
    }

    @Test("Shift decides the case a prediction has no prefix to take")
    func shiftCapitalizesAPrediction() async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        var delivered: [[KeyboardSuggestionCandidate]] = []
        service.onCandidates = { _, values in if !values.isEmpty { delivered.append(values) } }
        _ = factory.configure(service)

        service.update(
            .init(prefix: "", completedToken: "xin", context: "xin"),
            canQuery: true,
            capitalized: true
        )
        let worker = try #require(factory.workers.first)
        worker.emit(["chào"], for: try #require(worker.events.compactMap(\.query).last))
        await Task.yield()

        #expect(delivered.last?.first?.text == "Chào")
    }

    private func update(prefix: String) -> KeyboardSuggestionInputUpdate {
        .init(prefix: prefix, completedToken: nil)
    }
}

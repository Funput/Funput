import Foundation
import KeyboardInput
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

    @Test("A prefix shorter than the query threshold never reaches the worker")
    func neverQueriesWithoutAPrefix() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        _ = factory.configure(service)

        service.update(.init(prefix: "", completedToken: "xin", context: "xin"), canQuery: true)
        service.update(.init(prefix: "c", completedToken: nil, context: "xin"), canQuery: true)

        let worker = try #require(factory.workers.first)
        // Without a prefix every follower matches, which is prediction without
        // the threshold that has to guard it.
        #expect(worker.events.compactMap(\.query).isEmpty)
    }

    private func update(prefix: String) -> KeyboardSuggestionInputUpdate {
        .init(prefix: prefix, completedToken: nil)
    }
}

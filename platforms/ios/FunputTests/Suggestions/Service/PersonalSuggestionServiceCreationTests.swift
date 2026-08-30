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

    private func update(prefix: String) -> KeyboardSuggestionInputUpdate {
        .init(prefix: prefix, completedToken: nil)
    }
}

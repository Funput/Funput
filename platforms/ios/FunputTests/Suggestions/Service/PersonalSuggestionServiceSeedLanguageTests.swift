import Foundation
import FunputEngine
import KeyboardInput
import PersonalSuggestions
import Testing

/// The service holds the composer's seed setting for the worker: across
/// configurations, for a worker created later, and as a change to a live one.
@MainActor
struct PersonalSuggestionServiceSeedLanguageTests {
    @Test("A worker created later starts with the setting")
    func laterWorkerStartsWithIt() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        service.setSeedLanguage(.vietnamese(.modern))
        factory.configure(service)

        service.update(.init(prefix: "", completedToken: "cảm", context: "cảm"), canQuery: true)

        let worker = try #require(factory.workers.first)
        let configuration = try #require(worker.events.compactMap(\.configuration).first)
        #expect(configuration.seedLanguage == .vietnamese(.modern))
    }

    @Test("Reconfiguring keeps the setting")
    func reconfigureKeepsIt() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service)
        service.update(.init(prefix: "", completedToken: "cảm", context: "cảm"), canQuery: true)
        service.setSeedLanguage(.vietnamese(.traditional))

        factory.configure(service, activation: 2)

        let worker = try #require(factory.workers.first)
        let configuration = try #require(worker.events.compactMap(\.configuration).last)
        #expect(configuration.seedLanguage == .vietnamese(.traditional))
    }

    @Test("A change reaches a live worker on its own, and a repeat does not")
    func changeReachesLiveWorker() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service)
        service.update(.init(prefix: "", completedToken: "cảm", context: "cảm"), canQuery: true)
        let worker = try #require(factory.workers.first)
        let configurations = worker.events.compactMap(\.configuration).count

        service.setSeedLanguage(.vietnamese(.modern))
        service.setSeedLanguage(.vietnamese(.modern))
        service.setSeedLanguage(.off)

        let changes = worker.events.compactMap { event -> SuggestionSeedLanguage? in
            guard case .setSeedLanguage(let language) = event else { return nil }
            return language
        }
        #expect(changes == [.vietnamese(.modern), .off])
        #expect(worker.events.compactMap(\.configuration).count == configurations)
    }
}

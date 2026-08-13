import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import Testing

@MainActor
struct PersonalSuggestionServiceLifecycleTests {
    @Test("Reactivation rejects stale results and reuses the worker")
    func rejectsStaleResult() async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        var delivered: [(UInt64, [KeyboardSuggestionCandidate])] = []
        service.onCandidates = { generation, values in
            if !values.isEmpty { delivered.append((generation, values)) }
        }
        _ = factory.configure(service, activation: 1)
        service.update(update(prefix: "kh"), canQuery: true)
        let worker = try #require(factory.workers.first)
        let stale = try #require(worker.events.compactMap(\.query).last)

        _ = factory.configure(service, activation: 2)
        worker.emit(["không"], for: stale)
        await Task.yield()
        #expect(delivered.isEmpty)

        service.update(update(prefix: "kh"), canQuery: true)
        let current = try #require(worker.events.compactMap(\.query).last)
        worker.emit(["không"], for: current)
        await Task.yield()

        #expect(factory.workers.count == 1)
        #expect(delivered.last?.0 == 2)
        #expect(delivered.last?.1.map(\.text) == ["không"])
    }

    @Test("Repeated activation applies new configuration to existing worker")
    func repeatedConfiguration() throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        _ = factory.configure(service, activation: 1)
        service.update(update(prefix: "kh"), canQuery: true)
        let worker = try #require(factory.workers.first)

        let latest = factory.configure(
            service,
            activation: 2,
            hasFullAccess: false
        )

        #expect(factory.workers.count == 1)
        #expect(worker.events.compactMap(\.configuration).last == latest)
    }

    private func update(prefix: String) -> KeyboardSuggestionInputUpdate {
        .init(prefix: prefix, completedToken: nil)
    }
}

import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import Testing

/// One suggestion service serves every host app in the keyboard's process. A new
/// session is the next app's typing: nothing of the last one may reach its bar.
@MainActor
struct PersonalSuggestionServiceSessionTests {
    @Test("A new session clears the bar and refuses what was still in flight")
    func sessionDropsThePreviousAnswer() async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        var delivered: [[String]] = []
        service.onCandidates = { _, values in delivered.append(values.map(\.text)) }
        factory.configure(service)
        service.update(update(prefix: "kh"), canQuery: true)
        let worker = try #require(factory.workers.first)
        let asked = try #require(worker.events.compactMap(\.query).last)
        worker.emit(["không"], for: asked)
        await Task.yield()
        #expect(delivered.last == ["không"])

        service.beginSession(hasFullAccess: true)
        #expect(delivered.last == [])

        worker.emit(["khi"], for: asked)
        await Task.yield()
        #expect(delivered.last == [])
    }

    /// With Full Access the store is on disk and is the same for every app, so there
    /// is nothing to gain from opening it again.
    @Test("With Full Access on both sides the worker is kept")
    func persistentWorkerIsKept() {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service, hasFullAccess: true)
        service.update(update(prefix: "kh"), canQuery: true)

        service.beginSession(hasFullAccess: true)
        factory.configure(service, hasFullAccess: true)
        service.update(update(prefix: "ng"), canQuery: true)

        #expect(factory.workers.count == 1)
    }

    /// Without it the learned words live in the worker's memory, and belong to the
    /// app they were typed in.
    @Test("Without Full Access on either side the worker is replaced", arguments: [
        (false, false), (false, true), (true, false),
    ])
    func inMemoryWorkerIsReplaced(before: Bool, after: Bool) throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service, hasFullAccess: before)
        service.update(update(prefix: "kh"), canQuery: true)
        let first = try #require(factory.workers.first)

        service.beginSession(hasFullAccess: after)
        factory.configure(service, hasFullAccess: after)
        service.update(update(prefix: "ng"), canQuery: true)

        #expect(factory.workers.count == 2)
        #expect(first.events.last == .flush)
    }

    private func update(prefix: String) -> KeyboardSuggestionInputUpdate {
        .init(prefix: prefix, completedToken: nil)
    }
}

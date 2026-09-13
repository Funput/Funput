import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import Testing

@MainActor
struct EnglishSuggestionCasingTests {
    @Test(arguments: [("ip", "iPhone"), ("IP", "IPHONE")])
    func preservesDictionaryCasing(_ example: (String, String)) async throws {
        let factory = SuggestionWorkerFactorySpy()
        let service = factory.makeService()
        factory.configure(service)
        let (stream, deliveries) = AsyncStream<[String]>.makeStream()
        var iterator = stream.makeAsyncIterator()
        service.onCandidates = { _, values in
            if !values.isEmpty { deliveries.yield(values.map(\.text)) }
        }
        service.update(.init(prefix: example.0, completedToken: nil), canQuery: true)
        let worker = try #require(factory.workers.first)
        let request = try #require(worker.events.compactMap(\.query).last)
        worker.emit(["iPhone"], for: request)
        #expect(await iterator.next() == [example.1])
        deliveries.finish()
    }
}

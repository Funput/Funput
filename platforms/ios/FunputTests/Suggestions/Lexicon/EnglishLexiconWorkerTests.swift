import Foundation
import PersonalSuggestions
import Testing

/// `which · when · what` is the committed `en.tsv` order; see EnglishLexiconBridgeTests.
@Suite(.timeLimit(.minutes(1)))
struct EnglishLexiconWorkerTests {
    @Test func attachesOncePerEngineAcrossFullAccessChanges() async throws {
        let url = try LexiconTestResource.url()
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let probes = LexiconURLProbe(url: url)
        let (stream, results) = AsyncStream<[String]>.makeStream()
        var iterator = stream.makeAsyncIterator()
        let worker = PersonalSuggestionWorker(
            lexiconURL: { probes.resolve() }, storeURL: { root }
        ) { _, values in results.yield(values.map(\.text)) }
        worker.configure(.init(enabled: true, hasFullAccess: false, resetToken: nil))
        worker.query(.init(prefix: "wh", generation: 1, context: nil))
        #expect(await iterator.next() == ["which", "when", "what"])
        #expect(probes.count == 1)
        worker.configure(.init(enabled: true, hasFullAccess: false, resetToken: nil))
        worker.query(.init(prefix: "wh", generation: 2, context: nil))
        #expect(await iterator.next() == ["which", "when", "what"])
        #expect(probes.count == 1)
        worker.configure(.init(enabled: true, hasFullAccess: true, resetToken: nil))
        worker.query(.init(prefix: "wh", generation: 3, context: nil))
        #expect(await iterator.next() == ["which", "when", "what"])
        #expect(probes.count == 2)
        worker.configure(.init(enabled: true, hasFullAccess: false, resetToken: nil))
        worker.query(.init(prefix: "wh", generation: 4, context: nil))
        #expect(await iterator.next() == ["which", "when", "what"])
        #expect(probes.count == 3)
        results.finish()
    }

    @Test func missingLexiconStillLearnsAndDisabledWorkerReturnsNothing() async {
        let (stream, results) = AsyncStream<[String]>.makeStream()
        var iterator = stream.makeAsyncIterator()
        let worker = PersonalSuggestionWorker(lexiconURL: { nil }, storeURL: { nil }) { _, values in
            results.yield(values.map(\.text))
        }
        worker.configure(.init(enabled: true, hasFullAccess: false, resetToken: nil))
        worker.learn("whimsy", after: nil)
        worker.learn("whimsy", after: nil)
        worker.query(.init(prefix: "wh", generation: 1, context: nil))
        #expect(await iterator.next() == ["whimsy"])
        worker.configure(.init(enabled: false, hasFullAccess: false, resetToken: nil))
        worker.query(.init(prefix: "wh", generation: 2, context: nil))
        #expect(await iterator.next() == [])
        results.finish()
    }
}

private final class LexiconURLProbe: @unchecked Sendable {
    private let lock = NSLock()
    private let url: URL
    private var calls = 0
    init(url: URL) { self.url = url }
    var count: Int { lock.withLock { calls } }
    func resolve() -> URL { lock.withLock { calls += 1; return url } }
}

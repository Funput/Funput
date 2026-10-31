import Foundation
import FunputEngine
import KeyboardInput
import PersonalSuggestions
import Testing

/// The shipped Vietnamese pairs through the real worker and native engine, on a store
/// that has learned nothing.
@Suite(.timeLimit(.minutes(1)))
struct SeedLanguageWorkerTests {
    private final class Generations: @unchecked Sendable {
        private let lock = NSLock()
        private var next: UInt64 = 0
        func take() -> UInt64 { lock.withLock { next += 1; return next } }
    }

    private let generations = Generations()

    private func predict(
        after previous: String,
        with worker: PersonalSuggestionWorker
    ) {
        worker.query(.init(prefix: "", generation: generations.take(), context: previous))
    }

    @Test func followsTheSettingAndAppliesAChangeBeforeTheNextQuery() async {
        let (stream, results) = AsyncStream<[String]>.makeStream()
        var iterator = stream.makeAsyncIterator()
        let worker = PersonalSuggestionWorker(lexiconURL: { nil }, storeURL: { nil }) { _, values in
            results.yield(values.map(\.text))
        }
        worker.configure(.init(enabled: true, hasFullAccess: false, resetToken: nil))
        predict(after: "cảm", with: worker)
        #expect(await iterator.next() == [])

        worker.setSeedLanguage(.vietnamese(.modern))
        predict(after: "sức", with: worker)
        #expect(await iterator.next() == ["khoẻ"])

        // Straight after a result: the drain may take this query in the same pass,
        // ahead of the block that applies the change. It must still see the change.
        worker.setSeedLanguage(.vietnamese(.traditional))
        predict(after: "sức", with: worker)
        #expect(await iterator.next() == ["khỏe"])

        worker.setSeedLanguage(.off)
        predict(after: "cảm", with: worker)
        #expect(await iterator.next() == [])
        results.finish()
    }

    @Test func theSettingSurvivesTheEngineBeingRecreated() async {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: root) }
        let (stream, results) = AsyncStream<[String]>.makeStream()
        var iterator = stream.makeAsyncIterator()
        let worker = PersonalSuggestionWorker(lexiconURL: { nil }, storeURL: { root }) { _, values in
            results.yield(values.map(\.text))
        }
        worker.configure(.init(
            enabled: true, hasFullAccess: false, resetToken: nil, seedLanguage: .vietnamese(.modern)
        ))
        predict(after: "cảm", with: worker)
        #expect(await iterator.next() == ["ơn"])

        // Full access opens the store on disk: a new engine, with the same setting.
        worker.configure(.init(
            enabled: true, hasFullAccess: true, resetToken: nil, seedLanguage: .vietnamese(.modern)
        ))
        predict(after: "tuy", with: worker)
        #expect(await iterator.next() == ["nhiên"])
        results.finish()
    }
}

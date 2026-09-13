import FunputShared
import KeyboardInput
import Testing

@MainActor
struct ShortcutLoaderTests {
    @Test func oldActivationCannotDeliverAfterNewActivation() async {
        let store = SuspendedShortcutStore()
        let loader = KeyboardShortcutsLoader(store: store)
        var deliveries: [ShortcutLibrary] = []
        let (stream, signal) = AsyncStream<ShortcutLibrary>.makeStream()
        var iterator = stream.makeAsyncIterator()
        let fresh = ShortcutLibrary(entries: [.init(trigger: "vn", expansion: "new")])
        loader.activate { deliveries.append($0); signal.yield($0) }
        await store.waitForLoads(1)
        loader.activate { deliveries.append($0); signal.yield($0) }
        await store.waitForLoads(2)
        await store.complete(0, with: .success(.init()))
        await store.complete(1, with: .success(fresh))
        #expect(await iterator.next() == fresh)
        #expect(deliveries == [fresh])
    }

    @Test func cancellationAndReadFailureKeepShortcutsOff() async {
        let store = SuspendedShortcutStore()
        let loader = KeyboardShortcutsLoader(store: store)
        var deliveries = 0
        let (stream, signal) = AsyncStream<Int>.makeStream()
        var iterator = stream.makeAsyncIterator()
        loader.activate { _ in deliveries += 1 }
        await store.waitForLoads(1)
        loader.cancel()
        await store.complete(0, with: .success(.init()))
        loader.activate { _ in deliveries += 1 }
        await store.waitForLoads(2)
        await store.complete(1, with: .failure(ShortcutsStorageError.readFailed))
        loader.activate { _ in
            deliveries += 1
            signal.yield(deliveries)
        }
        await store.waitForLoads(3)
        await store.complete(2, with: .success(.init()))
        #expect(await iterator.next() == 1)
        #expect(deliveries == 1)
    }
}

private actor SuspendedShortcutStore: ShortcutsStoring {
    private var next = 0
    private var reads: [Int: CheckedContinuation<ShortcutLibrary, any Error>] = [:]
    private var waiter: (Int, CheckedContinuation<Void, Never>)?

    func load() async throws -> ShortcutLibrary {
        let id = next
        next += 1
        return try await withCheckedThrowingContinuation { continuation in
            reads[id] = continuation
            if let (count, signal) = waiter, next >= count {
                waiter = nil
                signal.resume()
            }
        }
    }

    func waitForLoads(_ count: Int) async {
        if next >= count { return }
        await withCheckedContinuation { waiter = (count, $0) }
    }

    func complete(_ id: Int, with result: Result<ShortcutLibrary, any Error>) {
        reads.removeValue(forKey: id)?.resume(with: result)
    }

    func save(_ library: ShortcutLibrary) async throws { throw ShortcutsStorageError.writeFailed }
}

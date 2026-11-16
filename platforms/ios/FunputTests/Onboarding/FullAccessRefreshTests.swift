import FunputShared
import Testing
@testable import Funput

@MainActor
struct FullAccessRefreshTests {
    @Test("Starting reloads once and repeated starts do nothing")
    func startReloadsOnce() {
        let probe = RefreshProbe()
        let refresh = probe.makeRefresh()

        refresh.start()
        refresh.start()
        #expect(probe.reloads == 1)
        #expect(probe.signal.isObserving)

        refresh.stop()
        #expect(!probe.signal.isObserving)
        refresh.start()
        #expect(probe.reloads == 2)
    }

    @Test("A signal that finds the change does not retry")
    func signalWithChangeSettles() {
        let probe = RefreshProbe(changes: [true])
        let refresh = probe.makeRefresh()

        refresh.start()
        probe.signal.fire()
        #expect(probe.accessReads == 1)
        #expect(refresh.retryTask == nil)
    }

    @Test("A signal that reads a stale value retries until the change lands")
    func signalRetriesUntilChange() async {
        let probe = RefreshProbe(changes: [false, false, true])
        let refresh = probe.makeRefresh(retryAttempts: 5)

        refresh.start()
        probe.signal.fire()
        await refresh.retryTask?.value
        #expect(probe.accessReads == 3)
    }

    @Test("Retrying gives up after the configured attempts")
    func retryGivesUp() async {
        let probe = RefreshProbe()
        let refresh = probe.makeRefresh(retryAttempts: 2)

        refresh.start()
        probe.signal.fire()
        await refresh.retryTask?.value
        #expect(probe.accessReads == 3)
    }

    @Test("Stopping cancels a pending retry and ignores later signals")
    func stopCancelsRetry() async {
        let probe = RefreshProbe()
        let refresh = probe.makeRefresh(retryInterval: .seconds(60))

        refresh.start()
        probe.signal.fire()
        let pending = refresh.retryTask
        refresh.stop()
        await pending?.value
        probe.signal.fire()
        #expect(probe.accessReads == 1)
    }
}

/// Records what the refresh asks of the settings model, and answers access reads from a
/// scripted list of "changed" results (then `false` once the list runs out).
@MainActor
private final class RefreshProbe {
    let signal = TestSignal()
    private(set) var reloads = 0
    private(set) var accessReads = 0
    private var changes: [Bool]

    init(changes: [Bool] = []) {
        self.changes = changes
    }

    func makeRefresh(
        retryAttempts: Int = 3,
        retryInterval: Duration = .zero
    ) -> FullAccessRefresh {
        FullAccessRefresh(
            reloadSettings: { [unowned self] in reloads += 1 },
            refreshAccess: { [unowned self] in
                accessReads += 1
                return changes.isEmpty ? false : changes.removeFirst()
            },
            retryAttempts: retryAttempts,
            retryInterval: retryInterval,
            observe: signal.observe
        )
    }
}

@MainActor
private final class TestSignal {
    private var handler: (@MainActor () -> Void)?
    private weak var token: Token?

    var isObserving: Bool { token != nil }

    func observe(_ handler: @escaping @MainActor () -> Void) -> any ObservedFullAccessObserving {
        self.handler = handler
        let token = Token()
        self.token = token
        return token
    }

    func fire() { handler?() }

    private final class Token: ObservedFullAccessObserving {}
}

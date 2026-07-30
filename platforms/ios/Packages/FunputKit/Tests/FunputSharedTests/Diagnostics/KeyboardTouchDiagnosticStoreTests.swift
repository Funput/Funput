#if DEBUG
import Foundation
import FunputShared
import KeyboardLayout
import Testing

struct KeyboardTouchDiagnosticStoreTests {
    @Test("Session and matching report round-trip")
    func roundTrip() {
        withStore { store, _ in
            let session = makeSession(generation: 7)
            #expect(store.start(session))
            #expect(store.activeSession(now: session.startedAt) == session)

            let report = makeReport(session: session, sequence: 1)
            #expect(store.save(report, now: session.startedAt))
            #expect(store.report(now: session.startedAt) == report)
        }
    }

    @Test("New session removes previous report")
    func sessionIsolation() {
        withStore { store, _ in
            let first = makeSession(generation: 1)
            #expect(store.start(first))
            #expect(store.save(makeReport(session: first), now: first.startedAt))

            let second = makeSession(generation: 2)
            #expect(store.start(second))
            #expect(store.report(now: second.startedAt) == nil)
            #expect(!store.save(makeReport(session: first), now: second.startedAt))
        }
    }

    @Test("Expired and corrupt sessions are cleared")
    func invalidSession() {
        withStore { store, defaults in
            let session = makeSession(generation: 3)
            #expect(store.start(session))
            #expect(store.activeSession(now: session.expiresAt) == nil)

            defaults.set(Data([0, 1]), forKey: FunputAppGroup.touchDiagnosticSessionKey)
            #expect(store.activeSession(now: session.startedAt) == nil)
            #expect(defaults.data(forKey: FunputAppGroup.touchDiagnosticSessionKey) == nil)
        }
    }

    @Test("Clear removes both records")
    func clear() {
        withStore { store, _ in
            let session = makeSession(generation: 4)
            #expect(store.start(session))
            #expect(store.save(makeReport(session: session), now: session.startedAt))
            store.clear()
            #expect(store.activeSession(now: session.startedAt) == nil)
            #expect(store.report(now: session.startedAt) == nil)
        }
    }

    private func withStore(
        _ body: (KeyboardTouchDiagnosticStore, UserDefaults) -> Void
    ) {
        let name = "test.touch-diagnostic.\(UUID())"
        let defaults = UserDefaults(suiteName: name)!
        defer { defaults.removePersistentDomain(forName: name) }
        body(KeyboardTouchDiagnosticStore(defaults: defaults), defaults)
    }
}

private func makeSession(generation: UInt64) -> KeyboardTouchDiagnosticSession {
    let start = Date(timeIntervalSince1970: 1_000)
    return KeyboardTouchDiagnosticSession(
        inputMethod: .vni,
        phase: .guided,
        generation: generation,
        startedAt: start
    )
}

private func makeReport(
    session: KeyboardTouchDiagnosticSession,
    sequence: UInt64 = 0
) -> KeyboardTouchDiagnosticReport {
    KeyboardTouchDiagnosticReport(
        sessionID: session.id,
        generation: session.generation,
        sequence: sequence,
        observedAt: session.startedAt,
        metrics: .init(),
        activeContactCount: 0,
        pendingContactCount: 0,
        isSettled: true,
        device: .init(model: "iPhone", operatingSystem: "TestOS", maximumFramesPerSecond: 120)
    )
}
#endif

@testable import FunputShared
import Testing

@MainActor
@Suite("Clipboard polling while the user types")
struct ClipboardChangeMonitorPollingTests {
    /// A tick runs on the main thread while the user types, so an unchanged
    /// pasteboard must cost one query, not a full snapshot.
    @Test("Ticks on an unchanged pasteboard only read the change count")
    func ticksReadOnlyTheCount() {
        let probe = CountingPasteboard(snapshot: makeSnapshot(changeCount: 3))
        let monitor = probe.startMonitor()
        defer { monitor.stop() }
        for _ in 0..<10 { monitor.poll() }
        #expect(probe.snapshotReads == 1)
        #expect(probe.countReads == 10)
        #expect(probe.changes == 1)
    }

    @Test("A moved change count brings one full read and one change")
    func movedCountReadsInFull() {
        let probe = CountingPasteboard(snapshot: makeSnapshot(changeCount: 3))
        let monitor = probe.startMonitor()
        defer { monitor.stop() }
        probe.snapshot = makeSnapshot(changeCount: 4)
        monitor.poll()
        monitor.poll()
        #expect(probe.snapshotReads == 2)
        #expect(probe.changes == 2)
    }

    /// iOS declining to answer looks like an empty pasteboard at count 0, and the
    /// count alone would never move off it.
    @Test("An indeterminate answer is asked again in full")
    func indeterminateIsReadInFull() {
        let probe = CountingPasteboard(snapshot: makeSnapshot(changeCount: 0, hasPlainText: false))
        let monitor = probe.startMonitor()
        defer { monitor.stop() }
        monitor.poll()
        monitor.poll()
        #expect(probe.snapshotReads == 3)
        #expect(probe.countReads == 0)
    }

    private func makeSnapshot(changeCount: Int, hasPlainText: Bool = true) -> ClipboardSnapshot {
        ClipboardSnapshot(
            changeCount: changeCount, hasStrings: hasPlainText,
            hasURLs: false, hasPlainText: hasPlainText
        )
    }
}

/// A pasteboard that counts how the monitor asks it.
@MainActor
private final class CountingPasteboard {
    var snapshot: ClipboardSnapshot
    private(set) var snapshotReads = 0
    private(set) var countReads = 0
    private(set) var changes = 0

    init(snapshot: ClipboardSnapshot) { self.snapshot = snapshot }

    func startMonitor() -> ClipboardChangeMonitor {
        let monitor = ClipboardChangeMonitor()
        monitor.start(
            readSnapshot: { [unowned self] in snapshotReads += 1; return snapshot },
            readChangeCount: { [unowned self] in countReads += 1; return snapshot.changeCount },
            onChange: { [unowned self] in changes += 1 }
        )
        return monitor
    }
}

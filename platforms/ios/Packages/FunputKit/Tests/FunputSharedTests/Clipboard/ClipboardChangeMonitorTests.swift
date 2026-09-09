@testable import FunputShared
import Testing

@MainActor
@Suite("Clipboard changes during a keyboard session")
struct ClipboardChangeMonitorTests {
    @Test("The running monitor detects a copy without a keyboard callback")
    func automaticSampling() async throws {
        let monitor = ClipboardChangeMonitor()
        defer { monitor.stop() }
        var snapshot = ClipboardSnapshot(changeCount: 1, hasStrings: false, hasURLs: false)
        var changes = 0
        monitor.start(readSnapshot: { snapshot }, onChange: { changes += 1 })
        snapshot = ClipboardSnapshot(changeCount: 2, hasStrings: true, hasURLs: false)
        for _ in 0..<100 {
            if changes == 2 { break }
            try await Task.sleep(for: .milliseconds(20))
        }
        #expect(changes == 2)
    }

    @Test("Copy, replacement and clearing are detected without reopening the keyboard")
    func changesDuringSession() {
        let monitor = ClipboardChangeMonitor()
        defer { monitor.stop() }
        var snapshot = ClipboardSnapshot(changeCount: 1, hasStrings: false, hasURLs: false)
        var changes = 0
        monitor.start(readSnapshot: { snapshot }, onChange: { changes += 1 })
        #expect(changes == 1)
        monitor.sample()
        #expect(changes == 1)

        snapshot = ClipboardSnapshot(changeCount: 2, hasStrings: true, hasURLs: false)
        monitor.sample()
        #expect(changes == 2)
        snapshot = ClipboardSnapshot(changeCount: 3, hasStrings: true, hasURLs: false)
        monitor.sample()
        #expect(changes == 3)
        snapshot = ClipboardSnapshot(changeCount: 4, hasStrings: false, hasURLs: false)
        monitor.sample()
        #expect(changes == 4)
    }

    @Test("Hidden keyboards stop sampling and reopening refreshes unchanged metadata")
    func lifecycle() {
        let monitor = ClipboardChangeMonitor()
        defer { monitor.stop() }
        var reads = 0
        var changes = 0
        let read = {
            reads += 1
            return ClipboardSnapshot(changeCount: 5, hasStrings: true, hasURLs: false)
        }
        monitor.start(readSnapshot: read, onChange: { changes += 1 })
        monitor.stop()
        monitor.sample()
        #expect(reads == 1)
        #expect(changes == 1)
        monitor.start(readSnapshot: read, onChange: { changes += 1 })
        #expect(changes == 2)
    }

    @Test("Recovering access or an indeterminate read refreshes the offer")
    func recovery() {
        let monitor = ClipboardChangeMonitor()
        defer { monitor.stop() }
        var snapshot: ClipboardSnapshot?
        var changes = 0
        monitor.start(readSnapshot: { snapshot }, onChange: { changes += 1 })
        #expect(changes == 0)
        snapshot = ClipboardSnapshot(changeCount: 0, hasStrings: false, hasURLs: false)
        monitor.sample()
        snapshot = ClipboardSnapshot(changeCount: 9, hasStrings: true, hasURLs: false)
        monitor.sample()
        #expect(changes == 2)
        snapshot = nil
        monitor.sample()
        snapshot = ClipboardSnapshot(changeCount: 9, hasStrings: true, hasURLs: false)
        monitor.sample()
        #expect(changes == 3)
    }
}

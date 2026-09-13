#if canImport(UIKit)
@testable import FunputShared
import Testing
import UIKit

@MainActor
struct ClipboardNotificationTests {
    @Test func notificationsAndPollingShareOneChangeAndStopTogether() async throws {
        let monitor = ClipboardChangeMonitor()
        var count = 1
        var reads = 0
        var changes = 0
        monitor.start(readSnapshot: {
            reads += 1
            return ClipboardSnapshot(changeCount: count, hasStrings: true, hasURLs: false)
        }, onChange: { changes += 1 })
        count = 2
        NotificationCenter.default.post(name: UIPasteboard.changedNotification, object: nil)
        // Yield to the notification's MainActor handoff, without waiting for polling.
        for _ in 0..<100 where changes != 2 { await Task.yield() }
        #expect(changes == 2)
        monitor.sample()
        #expect(changes == 2)
        monitor.stop()
        let stoppedReads = reads
        NotificationCenter.default.post(name: UIPasteboard.changedNotification, object: nil)
        try await Task.sleep(for: .milliseconds(550))
        #expect(reads == stoppedReads)
    }
}
#endif

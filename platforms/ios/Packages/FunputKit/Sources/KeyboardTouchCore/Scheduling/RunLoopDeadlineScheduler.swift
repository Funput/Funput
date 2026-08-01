import Foundation

public enum RunLoopDeadlineScheduler {
    @MainActor
    public static func schedule(
        after delay: TimeInterval,
        action: @escaping @MainActor () -> Void
    ) -> ScheduledDeadline {
        let timer = Timer(timeInterval: max(0, delay), repeats: false) { _ in
            MainActor.assumeIsolated { action() }
        }
        RunLoop.main.add(timer, forMode: .common)
        return ScheduledDeadline { timer.invalidate() }
    }
}

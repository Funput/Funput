import Foundation

@MainActor
public final class ScheduledDeadline {
    private var cancellation: (() -> Void)?

    public init(cancellation: @escaping () -> Void) {
        self.cancellation = cancellation
    }

    public func cancel() {
        cancellation?()
        cancellation = nil
    }
}

public typealias DeadlineSchedule = @MainActor (
    _ delay: TimeInterval,
    _ action: @escaping @MainActor () -> Void
) -> ScheduledDeadline

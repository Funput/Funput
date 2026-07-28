import Foundation
import KeyboardTouchCore

@MainActor
final class TestDeadlineScheduler {
    final class Item {
        let deadline: TimeInterval
        let action: @MainActor () -> Void
        var isCancelled = false

        init(deadline: TimeInterval, action: @escaping @MainActor () -> Void) {
            self.deadline = deadline
            self.action = action
        }
    }

    var now: TimeInterval = 0
    private(set) var items: [Item] = []

    lazy var schedule: DeadlineSchedule = { [weak self] delay, action in
        guard let self else { return ScheduledDeadline {} }
        let item = Item(deadline: now + delay, action: action)
        items.append(item)
        return ScheduledDeadline { item.isCancelled = true }
    }

    var pendingCount: Int {
        items.count { !$0.isCancelled }
    }

    func runNext() {
        guard let item = items
            .filter({ !$0.isCancelled })
            .min(by: { $0.deadline < $1.deadline }) else { return }
        item.isCancelled = true
        now = item.deadline
        item.action()
    }

    func fireIncludingCancelled(at index: Int) {
        items[index].action()
    }
}

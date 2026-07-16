#if canImport(UIKit)
import KeyboardLayout

/// Serializes completed touch actions by touch-down order.
///
/// Overlapping fingers may lift in either order. Keeping completion separate
/// from emission prevents an older key from appearing after a newer key.
struct KeyboardPressCommitQueue {
    enum Completion {
        case released
        case cancelled
        case suppressed
        case swiped(KeySwipeAction)
    }

    enum ReadyAction {
        case event(KeyboardKeyEvent)
        case suppressed
    }

    private struct Entry {
        let key: KeySpec
        var completion: Completion?
        var bufferedRepeatCount = 0
    }

    private var entries: [Entry] = []

    var activeKey: KeySpec? {
        entries.last { $0.completion == nil }?.key
    }

    var isEmpty: Bool { entries.isEmpty }

    mutating func append(_ key: KeySpec) {
        entries.append(Entry(key: key))
    }

    func hasPendingKey(id: String) -> Bool {
        pendingIndex(for: id) != nil
    }

    @discardableResult
    mutating func complete(id: String, as completion: Completion) -> Bool {
        guard let index = pendingIndex(for: id) else { return false }
        entries[index].completion = completion
        return true
    }

    mutating func cancelAll() {
        for index in entries.indices where entries[index].completion == nil {
            entries[index].completion = .cancelled
        }
    }

    @discardableResult
    mutating func bufferRepeat(for id: String) -> Bool {
        guard let index = pendingIndex(for: id) else { return false }
        entries[index].bufferedRepeatCount += 1
        return true
    }

    /// Removes one ready action from the head. A nil result means the queue is
    /// empty or its oldest touch has not finished yet.
    mutating func popReadyAction() -> ReadyAction? {
        guard let first = entries.first else { return nil }
        if first.bufferedRepeatCount > 0 {
            entries[0].bufferedRepeatCount -= 1
            return .event(KeyboardKeyEvent(key: first.key, phase: .repeated))
        }
        guard let completion = first.completion else { return nil }
        entries.removeFirst()
        switch completion {
        case .released:
            return .event(KeyboardKeyEvent(key: first.key, phase: .released))
        case .cancelled:
            return .event(KeyboardKeyEvent(key: first.key, phase: .cancelled))
        case .suppressed:
            return .suppressed
        case let .swiped(action):
            return .event(KeyboardKeyEvent(key: first.key, phase: .swiped(action)))
        }
    }

    private func pendingIndex(for id: String) -> Int? {
        entries.firstIndex { $0.key.id == id && $0.completion == nil }
    }
}
#endif

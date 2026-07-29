#if canImport(UIKit)
import KeyboardLayout

/// Serializes completed touch actions by touch-down order.
///
/// Overlapping fingers may lift in either order. Keeping completion separate
/// from emission prevents an older key from appearing after a newer key.
struct KeyboardPressCommitQueue {
    typealias TouchToken = UInt64

    enum Completion {
        case released
        case cancelled
        case suppressed
        case swiped(KeySwipeAction)
        case alternate(KeyAlternate)
    }

    enum ReadyAction {
        case event(TouchToken, KeyboardKeyEvent)
        case suppressed
    }

    private struct Entry {
        let token: TouchToken
        var key: KeySpec
        var completion: Completion?
        var bufferedRepeatCount = 0
    }

    private var entries: [Entry] = []
    private var head = 0

    init() {
        entries.reserveCapacity(10)
    }

    var activeKey: KeySpec? {
        entries[head...].last { $0.completion == nil }?.key
    }

    var isEmpty: Bool { head == entries.count }
    var pendingTokens: Set<TouchToken> {
        Set(entries[head...].lazy.filter { $0.completion == nil }.map(\.token))
    }
    var depth: Int { entries.count - head }

    mutating func append(token: TouchToken, key: KeySpec) {
        precondition(index(for: token) == nil, "A touch token may only be queued once")
        entries.append(Entry(token: token, key: key))
    }

    func hasPendingTouch(_ token: TouchToken) -> Bool {
        pendingIndex(for: token) != nil
    }

    @discardableResult
    mutating func update(token: TouchToken, key: KeySpec) -> Bool {
        guard let index = pendingIndex(for: token) else { return false }
        entries[index].key = key
        return true
    }

    @discardableResult
    mutating func complete(token: TouchToken, as completion: Completion) -> Bool {
        guard let index = pendingIndex(for: token) else { return false }
        entries[index].completion = completion
        return true
    }

    mutating func cancelAll() {
        for index in head..<entries.count where entries[index].completion == nil {
            entries[index].completion = .cancelled
        }
    }

    @discardableResult
    mutating func bufferRepeat(for token: TouchToken) -> Bool {
        guard let index = pendingIndex(for: token) else { return false }
        entries[index].bufferedRepeatCount += 1
        return true
    }

    /// Removes one ready action from the head. A nil result means the queue is
    /// empty or its oldest touch has not finished yet.
    mutating func popReadyAction() -> ReadyAction? {
        guard head < entries.count else { return nil }
        let first = entries[head]
        if first.bufferedRepeatCount > 0 {
            entries[head].bufferedRepeatCount -= 1
            return .event(
                first.token,
                KeyboardKeyEvent(key: first.key, phase: .repeated)
            )
        }
        guard let completion = first.completion else { return nil }
        head += 1
        compactIfNeeded()
        switch completion {
        case .released:
            return .event(first.token, KeyboardKeyEvent(key: first.key, phase: .released))
        case .cancelled:
            return .event(first.token, KeyboardKeyEvent(key: first.key, phase: .cancelled))
        case .suppressed:
            return .suppressed
        case let .swiped(action):
            return .event(
                first.token,
                KeyboardKeyEvent(key: first.key, phase: .swiped(action))
            )
        case let .alternate(alternate):
            return .event(
                first.token,
                KeyboardKeyEvent(key: first.key, phase: .alternateSelected(alternate))
            )
        }
    }

    private func index(for token: TouchToken) -> Int? {
        entries[head...].firstIndex { $0.token == token }
    }

    private func pendingIndex(for token: TouchToken) -> Int? {
        entries[head...].firstIndex { $0.token == token && $0.completion == nil }
    }

    private mutating func compactIfNeeded() {
        if head == entries.count {
            entries.removeAll(keepingCapacity: true)
            head = 0
            return
        }
        guard head >= 64 else { return }
        entries.removeFirst(head)
        head = 0
    }
}
#endif

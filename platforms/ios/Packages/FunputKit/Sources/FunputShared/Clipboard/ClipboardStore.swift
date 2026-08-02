import Foundation

/// Clipboard history, persisted as a file in the App Group container.
///
/// A file rather than `UserDefaults` (which is what ``EmojiRecentsStore`` uses)
/// because `isExcludedFromBackup` is a property of a file, not of one key inside a
/// plist — and keeping copied passwords out of device backups is the whole reason
/// this store is separate.
public struct ClipboardStore {
    public static let limit = 50
    /// Unpinned entries fall away after this long. Pinned entries never expire.
    public static let expiry: TimeInterval = 60 * 60

    private struct Payload: Codable {
        var lastCapturedChangeCount: Int?
        var items: [ClipboardItem]

        static let empty = Payload(lastCapturedChangeCount: nil, items: [])
    }

    private let fileURL: URL?

    public init(directoryName: String = FunputAppGroup.clipboardDirectory) {
        self.init(directory: AppGroupDirectory.prepare(named: directoryName))
    }

    public init(directory: URL?) {
        fileURL = directory?.appendingPathComponent("clipboard.json")
    }

    /// Entries still worth showing. Pruning happens in memory: reading should not
    /// have the side effect of writing.
    public func load(now: Date = Date()) -> [ClipboardItem] {
        prune(read().items, now: now)
    }

    /// The pasteboard generation whose text is already stored, so the keyboard does
    /// not offer the same item twice. Survives the item being deleted or expiring.
    public func lastCapturedChangeCount() -> Int? {
        read().lastCapturedChangeCount
    }

    @discardableResult
    public func record(_ item: ClipboardItem, now: Date = Date()) -> [ClipboardItem] {
        var payload = read()
        payload.lastCapturedChangeCount = item.sourceChangeCount
        payload.items = prune([item] + payload.items.filter { $0.text != item.text }, now: now)
        write(payload)
        return payload.items
    }

    @discardableResult
    public func setPinned(_ isPinned: Bool, id: UUID, now: Date = Date()) -> [ClipboardItem] {
        var payload = read()
        guard let index = payload.items.firstIndex(where: { $0.id == id }) else {
            return prune(payload.items, now: now)
        }
        payload.items[index].isPinned = isPinned
        payload.items = prune(payload.items, now: now)
        write(payload)
        return payload.items
    }

    @discardableResult
    public func remove(id: UUID, now: Date = Date()) -> [ClipboardItem] {
        var payload = read()
        payload.items = prune(payload.items.filter { $0.id != id }, now: now)
        write(payload)
        return payload.items
    }

    /// Drops every entry, pinned included. `lastCapturedChangeCount` is cleared too,
    /// so whatever is still on the pasteboard can be offered again — after an
    /// explicit wipe the user should be able to start over.
    public func clear() {
        write(.empty)
    }

    /// Newest first. Expired unpinned entries go, then the oldest unpinned entries
    /// go until the list fits; pinned entries are never evicted.
    private func prune(_ items: [ClipboardItem], now: Date) -> [ClipboardItem] {
        let live = items.filter { $0.isPinned || now.timeIntervalSince($0.capturedAt) < Self.expiry }
        guard live.count > Self.limit else { return live }
        var excess = live.count - Self.limit
        var kept: [ClipboardItem] = []
        for item in live.reversed() {
            if excess > 0, !item.isPinned {
                excess -= 1
                continue
            }
            kept.append(item)
        }
        return Array(kept.reversed())
    }

    private func read() -> Payload {
        guard let fileURL, let data = try? Data(contentsOf: fileURL) else { return .empty }
        return (try? JSONDecoder().decode(Payload.self, from: data)) ?? .empty
    }

    private func write(_ payload: Payload) {
        guard let fileURL, let data = try? JSONEncoder().encode(payload) else { return }
        try? data.write(
            to: fileURL,
            options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication]
        )
    }
}

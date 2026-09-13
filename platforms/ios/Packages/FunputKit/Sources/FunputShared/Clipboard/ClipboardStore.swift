import Foundation

/// Clipboard history, persisted as a file in the App Group container.
///
/// A file rather than `UserDefaults` (which is what ``EmojiRecentsStore`` uses)
/// because `isExcludedFromBackup` is a property of a file, not of one key inside a
/// plist — and keeping copied passwords out of device backups is the whole reason
/// this store is separate.
public struct ClipboardStore {
    public static let limit = 50

    /// The change-count marks and the boot they belong to. On disk, not in the
    /// session: the keyboard is rebuilt in every app it appears in.
    struct Payload: Codable {
        var bootTime: Int?
        var lastCapturedChangeCount: Int?
        var lastPastedChangeCount: Int?
        var items: [ClipboardItem]

        static let empty = Payload(
            bootTime: nil, lastCapturedChangeCount: nil, lastPastedChangeCount: nil, items: []
        )

        /// Claims the marks for this boot, dropping any the previous boot left: their
        /// change counts name generations that no longer exist and could collide.
        mutating func stampThisBoot() {
            if !marksAreCurrent {
                lastCapturedChangeCount = nil
                lastPastedChangeCount = nil
            }
            bootTime = ClipboardBootTime.current
        }

        /// Change counts restart at boot, so marks written before this one name
        /// generations that no longer exist.
        var marksAreCurrent: Bool {
            guard let bootTime, let current = ClipboardBootTime.current else { return false }
            return bootTime == current
        }
    }

    private let fileURL: URL?
    /// How long an unpinned entry survives. Comes from the user's settings, so the
    /// store carries it rather than hard-coding one.
    private let expiry: TimeInterval

    public init(
        directoryName: String = FunputAppGroup.clipboardDirectory,
        expiry: ClipboardExpiry = .hour
    ) {
        self.init(directory: AppGroupDirectory.prepare(named: directoryName), expiry: expiry)
    }

    public init(directory: URL?, expiry: ClipboardExpiry = .hour) {
        fileURL = directory?.appendingPathComponent("clipboard.json")
        self.expiry = expiry.interval
    }

    /// Entries still worth showing. Pruning happens in memory: reading should not
    /// write.
    public func load(now: Date = Date()) -> [ClipboardItem] {
        prune(read().items, now: now)
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

    /// Drops every entry, pinned included, and both change-count marks with them:
    /// after a wipe nothing on disk claims to have seen this pasteboard. Not
    /// re-importing it is the running session's job, not the store's.
    public func clear() {
        write(.empty)
    }

    /// Newest first. Expired unpinned entries go, then the oldest unpinned ones until
    /// the list fits; pinned entries are never evicted.
    private func prune(_ items: [ClipboardItem], now: Date) -> [ClipboardItem] {
        let live = items.filter { $0.isPinned || now.timeIntervalSince($0.capturedAt) < expiry }
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

    /// Idempotent for an unchanged clipboard, including a reopened keyboard.
    public func capture(_ item: ClipboardItem, now: Date = Date()) -> Bool {
        var payload = read()
        let known = payload.items.contains {
            $0.text == item.text && $0.sourceChangeCount == item.sourceChangeCount
        }
        // Re-stamp even for text already held: the mark is what spares the next
        // session a pasteboard read, and after a boot it has to be earned again.
        guard !known || !payload.marksAreCurrent
            || payload.lastCapturedChangeCount != item.sourceChangeCount else { return true }
        payload.stampThisBoot()
        payload.lastCapturedChangeCount = item.sourceChangeCount
        guard !known else { return write(payload) }
        payload.items = merged(item, into: payload.items, now: now)
        return write(payload)
    }

    private func merged(_ item: ClipboardItem, into items: [ClipboardItem], now: Date) -> [ClipboardItem] {
        let old = items.first { $0.text == item.text }
        let updated = ClipboardItem(
            id: old?.id ?? item.id, text: item.text, capturedAt: item.capturedAt,
            isPinned: old?.isPinned ?? item.isPinned, sourceChangeCount: item.sourceChangeCount
        )
        return prune([updated] + items.filter { $0.text != item.text }, now: now)
    }

    func read() -> Payload {
        guard let fileURL, let data = try? Data(contentsOf: fileURL) else { return .empty }
        return (try? JSONDecoder().decode(Payload.self, from: data)) ?? .empty
    }

    @discardableResult
    func write(_ payload: Payload) -> Bool {
        guard let fileURL, let data = try? JSONEncoder().encode(payload) else { return false }
        do {
            try data.write(to: fileURL, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            return true
        } catch { return false }
    }
}

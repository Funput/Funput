import Foundation

/// Clipboard history, persisted as a file in the App Group container.
///
/// A file rather than `UserDefaults` (which is what ``EmojiRecentsStore`` uses)
/// because `isExcludedFromBackup` is a property of a file, not of one key inside a
/// plist — and keeping copied passwords out of device backups is the whole reason
/// this store is separate.
public struct ClipboardStore {
    public static let limit = 50

    /// The pasted mark and the boot it belongs to. On disk, not in the session: the
    /// keyboard is rebuilt in every app it appears in. Files written while the
    /// keyboard still captured on its own also carry `lastCapturedChangeCount`;
    /// decoding ignores it.
    struct Payload: Codable {
        var bootTime: Int?
        var lastPastedChangeCount: Int?
        var items: [ClipboardItem]

        static let empty = Payload(bootTime: nil, lastPastedChangeCount: nil, items: [])

        /// Claims the mark for this boot, dropping one the previous boot left: its
        /// change count names a generation that no longer exists and could collide.
        mutating func stampThisBoot() {
            if !marksAreCurrent { lastPastedChangeCount = nil }
            bootTime = ClipboardBootTime.current
        }

        /// Change counts restart at boot, so a mark written before this one names a
        /// generation that no longer exists.
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

    /// Drops every entry, pinned included, and the pasted mark with them: after a wipe
    /// nothing on disk claims to have seen this pasteboard.
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

    /// Adds a pasted entry, or moves an earlier copy of the same text to the front.
    /// Idempotent for the same text from the same generation, so pasting it twice
    /// writes nothing the second time.
    public func record(_ item: ClipboardItem, now: Date = Date()) -> Bool {
        var payload = read()
        let known = payload.items.contains {
            $0.text == item.text && $0.sourceChangeCount == item.sourceChangeCount
        }
        guard !known else { return true }
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

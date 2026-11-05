import Foundation

/// Clipboard history, persisted as a file in the App Group container.
///
/// A file rather than `UserDefaults` (which is what ``EmojiRecentsStore`` uses)
/// because `isExcludedFromBackup` is a property of a file, not of one key inside a
/// plist — and keeping copied passwords out of device backups is the whole reason
/// this store is separate.
public struct ClipboardStore {
    public static let limit = 50

    /// The history file. `bootTime` and `lastPastedChangeCount` are what older builds
    /// kept here before the mark moved to its own file (``ClipboardMarks``); they are
    /// only ever read, once, to migrate it, and never written again. Files from the
    /// builds that captured on their own also carry `lastCapturedChangeCount`, which
    /// decoding ignores.
    struct Payload: Codable {
        var bootTime: Int?
        var lastPastedChangeCount: Int?
        var items: [ClipboardItem]

        static let empty = Payload(bootTime: nil, lastPastedChangeCount: nil, items: [])
    }

    private let fileURL: URL?
    /// Next to the history rather than inside it, so reading the mark never decodes
    /// the history. See `ClipboardStoreMarks.swift`.
    let marksURL: URL?
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
        marksURL = directory?.appendingPathComponent("clipboard-marks.json")
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
    /// nothing on disk claims to have seen this pasteboard. The mark is emptied rather
    /// than deleted, so the next read does not go looking for one to migrate.
    public func clear() {
        write(.empty)
        writeMarks(.empty)
    }

    /// Expired unpinned entries go, then whatever ``ClipboardHistoryPolicy`` says no
    /// longer fits.
    private func prune(_ items: [ClipboardItem], now: Date) -> [ClipboardItem] {
        ClipboardHistoryPolicy.prune(items, limit: Self.limit) {
            $0.isPinned || now.timeIntervalSince($0.capturedAt) < expiry
        }
    }

    /// Adds a pasted entry, or moves an earlier copy of the same text to the front.
    /// Idempotent for the same text from the same generation, so pasting it twice
    /// writes nothing the second time. Returns whether the history holds the entry
    /// afterwards: false for text too long to keep, for text that does not fit beside
    /// the pinned entries, and when the write fails. The history is left as it was.
    public func record(_ item: ClipboardItem, now: Date = Date()) -> Bool {
        var payload = read()
        let known = payload.items.contains {
            $0.text == item.text && $0.sourceChangeCount == item.sourceChangeCount
        }
        guard !known else { return true }
        let items = merged(item, into: payload.items, now: now)
        guard items.contains(where: { $0.text == item.text }) else { return false }
        payload.items = items
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

    /// Drops the legacy mark on the way out: once the history is rewritten, the only
    /// copy of the mark is the one in its own file.
    @discardableResult
    func write(_ payload: Payload) -> Bool {
        var payload = payload
        payload.bootTime = nil
        payload.lastPastedChangeCount = nil
        guard let fileURL, let data = try? JSONEncoder().encode(payload) else { return false }
        return Self.persist(data, to: fileURL)
    }

    /// Both files hold what the user pasted, or a trace of it, so both get the same
    /// protection class. The directory already keeps them out of backups.
    static func persist(_ data: Data, to url: URL) -> Bool {
        do {
            try data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            return true
        } catch { return false }
    }
}

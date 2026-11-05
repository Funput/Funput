import Foundation

/// How much text the clipboard history may hold.
///
/// Every save decodes the whole history and encodes it again on the main actor, and
/// the keyboard extension runs close to its jetsam limit, so the history needs a
/// ceiling that does not depend on what the user pastes. Sizes are UTF-16 code
/// units: a pasted `NSString` answers `utf16.count` without walking the text, where
/// `String.count` would count every grapheme on the main thread. Precomposed
/// Vietnamese is one unit per letter and at most three UTF-8 bytes, so a full
/// history stays under about 768 KB on disk.
public enum ClipboardHistoryPolicy {
    /// Longer text is still pasted, just not kept: a history entry cut short would
    /// paste back as silently lost text, which is worse than no entry at all.
    public static let maxItemUTF16 = 16_384
    /// Pinned entries count too, but are never evicted for it; a new entry that does
    /// not fit beside them is the one left out.
    public static let maxTotalUTF16 = 262_144

    public static func admits(_ text: String) -> Bool {
        text.utf16.count <= maxItemUTF16
    }

    /// Newest first. Drops entries that are no longer live or too long to keep,
    /// including any written before these limits existed, then the oldest unpinned
    /// ones until both the entry count and the total size fit. Pinned entries are
    /// never evicted.
    static func prune(
        _ items: [ClipboardItem],
        limit: Int,
        isLive: (ClipboardItem) -> Bool
    ) -> [ClipboardItem] {
        var kept = items.filter { isLive($0) && admits($0.text) }
        var total = kept.reduce(0) { $0 + $1.text.utf16.count }
        var index = kept.count - 1
        while index >= 0, kept.count > limit || total > maxTotalUTF16 {
            if !kept[index].isPinned {
                total -= kept[index].text.utf16.count
                kept.remove(at: index)
            }
            index -= 1
        }
        return kept
    }
}

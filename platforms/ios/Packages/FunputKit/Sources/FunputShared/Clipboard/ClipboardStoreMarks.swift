import Foundation

/// What the keyboard remembers about the *current* pasteboard between sessions.
///
/// Kept apart from the history itself, in a file of its own, for two reasons. The
/// lifetime differs: entries are the user's data, while this number exists only to
/// keep the toolbar from inviting a paste the user already made, and has to outlive
/// the session because the keyboard is rebuilt in every app. And the cost differs:
/// the keyboard reads this on its way up, so it must cost a few bytes, not a decode
/// of everything the user ever pasted.
public extension ClipboardStore {
    /// Already pasted: the offer stays down until the user copies something else.
    /// Pasting the same thing again is what the history panel is for. Does not
    /// survive a reboot, where the number means something else.
    func lastPastedChangeCount() -> Int? {
        let marks = readMarks()
        return marks.marksAreCurrent ? marks.lastPastedChangeCount : nil
    }

    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool {
        var marks = readMarks()
        guard !marks.marksAreCurrent || marks.lastPastedChangeCount != changeCount else {
            return true
        }
        marks.stampThisBoot()
        marks.lastPastedChangeCount = changeCount
        return writeMarks(marks)
    }
}

extension ClipboardStore {
    /// The mark file, or, the first time a build with it runs, the mark an older build
    /// left inside the history. That one-off migration is the only read of the history
    /// this path ever makes; afterwards the mark file always exists.
    func readMarks() -> ClipboardMarks {
        guard let marksURL else { return .empty }
        if let data = try? Data(contentsOf: marksURL) {
            return (try? JSONDecoder().decode(ClipboardMarks.self, from: data)) ?? .empty
        }
        let legacy = read()
        let marks = ClipboardMarks(
            bootTime: legacy.bootTime, lastPastedChangeCount: legacy.lastPastedChangeCount
        )
        writeMarks(marks)
        return marks
    }

    @discardableResult
    func writeMarks(_ marks: ClipboardMarks) -> Bool {
        guard let marksURL, let data = try? JSONEncoder().encode(marks) else { return false }
        return Self.persist(data, to: marksURL)
    }
}

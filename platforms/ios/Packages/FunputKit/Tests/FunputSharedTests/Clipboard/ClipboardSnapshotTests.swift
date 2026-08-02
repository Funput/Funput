import FunputShared
import Testing

@Suite("Clipboard snapshot")
struct ClipboardSnapshotTests {
    /// iOS answers a refused read (`PBErrorDomain` code 10) as though the pasteboard
    /// were empty and untouched since boot, so that shape must not be believed.
    @Test("An all-zero reading is treated as indeterminate")
    func refusedRead() {
        let snapshot = ClipboardSnapshot(changeCount: 0, hasStrings: false, hasURLs: false)
        #expect(snapshot.isIndeterminate)
    }

    @Test("A pasteboard the user emptied is a real answer, not a refusal")
    func genuinelyEmptied() {
        let snapshot = ClipboardSnapshot(changeCount: 184, hasStrings: false, hasURLs: false)
        #expect(!snapshot.isIndeterminate)
    }

    @Test("Any content at all makes the reading trustworthy")
    func withContent() {
        #expect(
            !ClipboardSnapshot(changeCount: 0, hasStrings: true, hasURLs: false).isIndeterminate
        )
        #expect(
            !ClipboardSnapshot(changeCount: 0, hasStrings: false, hasURLs: true).isIndeterminate
        )
    }
}

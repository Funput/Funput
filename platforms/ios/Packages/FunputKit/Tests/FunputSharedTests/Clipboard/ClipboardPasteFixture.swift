import FunputShared
import KeyboardLayout

/// Pasteboard metadata a test can move forward, the way a new copy does.
@MainActor
final class PasteGateway: ClipboardGateway {
    var metadata = ClipboardSnapshot(
        changeCount: 1, hasStrings: true, hasURLs: false, hasPlainText: true
    )
    func snapshot() -> ClipboardSnapshot { metadata }
    func copy() {
        metadata = ClipboardSnapshot(
            changeCount: metadata.changeCount + 1,
            hasStrings: true, hasURLs: false, hasPlainText: true
        )
    }
}

/// Stands in for the on-disk mark, so a test can end one session and start another
/// the way switching apps does.
final class PastedMarkSpy: ClipboardSessionMarkStoring {
    private(set) var pasted: Int?
    func lastPastedChangeCount() -> Int? { pasted }
    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool {
        pasted = changeCount
        return true
    }
}

@MainActor
final class PasteFixture {
    let gateway = PasteGateway()
    let marks = PastedMarkSpy()
    var allowed = true
    var writesSucceed = true
    var saved: [ClipboardItem] = []
    var updates = 0
    lazy var recorder = ClipboardPasteRecorder(
        gateway: gateway, allowsRecording: { [unowned self] in allowed },
        save: { [unowned self] item in
            guard writesSucceed else { return false }
            saved.append(item)
            return true
        },
        marks: marks,
        onUpdate: { [unowned self] in updates += 1 }
    )
    init() { recorder.begin() }

    /// Ends this session and starts the next, as reopening the keyboard in another
    /// app does.
    func reopen() {
        recorder.end()
        recorder.begin()
    }

    func offer() -> ClipboardOffer? {
        ClipboardOfferPolicy.offer(
            snapshot: gateway.metadata,
            lastPastedChangeCount: recorder.lastPastedChangeCount,
            context: ClipboardOfferPolicy.Context(
                editorMode: .text, hasToolbar: true, hasFullAccess: true
            )
        )
    }
}

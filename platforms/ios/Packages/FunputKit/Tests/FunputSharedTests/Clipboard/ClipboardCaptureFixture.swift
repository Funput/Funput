import FunputShared

@MainActor
final class CaptureGateway: ClipboardGateway {
    var metadata = ClipboardSnapshot(changeCount: 1, hasStrings: true, hasURLs: false)
    var text: String? = "  Tiếng Việt\n🙂  "
    var reads = 0
    var duringRead: (() -> Void)?
    func snapshot() -> ClipboardSnapshot { metadata }
    func readText() -> String? { reads += 1; duringRead?(); return text }
    func copy(_ text: String?) {
        self.text = text
        metadata = ClipboardSnapshot(
            changeCount: metadata.changeCount + 1, hasStrings: text != nil, hasURLs: false
        )
    }
}

/// Stands in for the on-disk marks, so a test can end one session and start another
/// the way switching apps does.
final class SessionMarkSpy: ClipboardSessionMarkStoring {
    var captured: Int?
    private var pasted: Int?
    func lastCapturedChangeCount() -> Int? { captured }
    func lastPastedChangeCount() -> Int? { pasted }
    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool {
        pasted = changeCount
        return true
    }
}

@MainActor
final class CaptureFixture {
    let gateway = CaptureGateway()
    let marks = SessionMarkSpy()
    var allowed = true
    var writesSucceed = true
    var saved: [ClipboardItem] = []
    var updates = 0
    lazy var controller = ClipboardCaptureController(
        gateway: gateway, allowsCapture: { [unowned self] in allowed },
        save: { [unowned self] item in
            guard writesSucceed else { return false }
            saved.append(item)
            return true
        },
        marks: marks,
        onUpdate: { [unowned self] in updates += 1 }
    )
    init() { controller.begin() }
}

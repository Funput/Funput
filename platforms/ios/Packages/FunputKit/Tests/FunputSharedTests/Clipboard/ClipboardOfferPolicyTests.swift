import FunputShared
import KeyboardLayout
import Testing

@Suite("Clipboard offer policy")
struct ClipboardOfferPolicyTests {
    private let text = ClipboardSnapshot(changeCount: 42, hasStrings: true, hasURLs: false)

    @Test("Offers plain text when every precondition holds")
    func offersText() {
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: text, lastCapturedChangeCount: 41, context: context()
            ) == ClipboardOffer(kind: .text, changeCount: 42)
        )
    }

    @Test("A copied URL is announced as a link")
    func offersLink() {
        let snapshot = ClipboardSnapshot(changeCount: 7, hasStrings: true, hasURLs: true)
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: snapshot, lastCapturedChangeCount: nil, context: context()
            )?.kind == .link
        )
    }

    @Test("Nothing captured yet still counts as new")
    func noHistory() {
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: text, lastCapturedChangeCount: nil, context: context()
            ) != nil
        )
    }

    @Test(
        "Password and PIN fields never see an offer",
        arguments: [KeyboardEditorMode.password, .pin]
    )
    func neverInSecureFields(_ mode: KeyboardEditorMode) {
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: text,
                lastCapturedChangeCount: nil,
                context: context(editorMode: mode)
            ) == nil
        )
    }

    @Test("Stays silent without Full Access")
    func withoutFullAccess() {
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: text,
                lastCapturedChangeCount: nil,
                context: context(hasFullAccess: false)
            ) == nil
        )
    }

    @Test("Stays silent when the layout has no toolbar to host the chip")
    func withoutToolbar() {
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: text,
                lastCapturedChangeCount: nil,
                context: context(hasToolbar: false)
            ) == nil
        )
    }

    @Test("An image-only pasteboard is ignored in v1")
    func withoutStrings() {
        let snapshot = ClipboardSnapshot(changeCount: 9, hasStrings: false, hasURLs: false)
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: snapshot, lastCapturedChangeCount: nil, context: context()
            ) == nil
        )
    }

    @Test("The same pasteboard generation is never offered twice")
    func alreadyCaptured() {
        #expect(
            ClipboardOfferPolicy.offer(
                snapshot: text, lastCapturedChangeCount: 42, context: context()
            ) == nil
        )
    }

    /// The surroundings check has to stand on its own: the controller applies it even
    /// when a refused pasteboard read leaves it with no snapshot to judge, which is
    /// what keeps a stale chip out of a password field.
    @Test("Surroundings alone rule out password fields, missing toolbar and access")
    func surroundingsGate() {
        #expect(ClipboardOfferPolicy.allowsOffer(context: context()))
        #expect(!ClipboardOfferPolicy.allowsOffer(context: context(editorMode: .password)))
        #expect(!ClipboardOfferPolicy.allowsOffer(context: context(editorMode: .pin)))
        #expect(!ClipboardOfferPolicy.allowsOffer(context: context(hasToolbar: false)))
        #expect(!ClipboardOfferPolicy.allowsOffer(context: context(hasFullAccess: false)))
        #expect(!ClipboardOfferPolicy.allowsOffer(context: context(isEnabled: false)))
    }

    private func context(
        editorMode: KeyboardEditorMode = .text,
        hasToolbar: Bool = true,
        hasFullAccess: Bool = true,
        isEnabled: Bool = true
    ) -> ClipboardOfferPolicy.Context {
        ClipboardOfferPolicy.Context(
            editorMode: editorMode,
            hasToolbar: hasToolbar,
            hasFullAccess: hasFullAccess,
            isEnabled: isEnabled
        )
    }
}

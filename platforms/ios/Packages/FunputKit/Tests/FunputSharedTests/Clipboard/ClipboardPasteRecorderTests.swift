import FunputShared
import Testing

@MainActor
struct ClipboardPasteRecorderTests {
    @Test func pasteIsRecordedUnderItsGenerationAndWithdrawsTheOffer() {
        let f = PasteFixture()
        #expect(f.offer() != nil)
        f.recorder.didPaste("Tiếng Việt", changeCount: 1)
        #expect(f.saved.map(\.text) == ["Tiếng Việt"])
        #expect(f.saved.first?.sourceChangeCount == 1)
        #expect(f.updates == 1)
        #expect(f.marks.pasted == 1)
        #expect(f.offer() == nil)
    }

    /// Switching apps rebuilds the keyboard. The offer must not come back with it.
    @Test func pasteSurvivesTheSessionAndOnlyANewCopyBringsTheOfferBack() {
        let f = PasteFixture()
        f.recorder.didPaste("một", changeCount: 1)
        f.reopen()
        #expect(f.recorder.lastPastedChangeCount == 1)
        #expect(f.offer() == nil)

        f.gateway.copy()
        f.reopen()
        #expect(f.offer() != nil)
    }

    /// The control delivered an older generation than the pasteboard now holds: the
    /// text is still kept, but the new copy must stay on offer.
    @Test func latePasteDoesNotClaimTheNewCopy() {
        let f = PasteFixture()
        f.gateway.copy()
        f.recorder.didPaste("cũ", changeCount: 1)
        #expect(f.saved.first?.sourceChangeCount == -1)
        #expect(f.recorder.lastPastedChangeCount == nil)
        #expect(f.marks.pasted == nil)
        #expect(f.offer() != nil)
    }

    @Test func unpinnedGenerationIsKeptWithoutClaimingOne() {
        let f = PasteFixture()
        f.recorder.didPaste("không rõ", changeCount: nil)
        #expect(f.saved.first?.sourceChangeCount == -1)
        #expect(f.recorder.lastPastedChangeCount == nil)
    }

    /// Recording follows the offer gate, which closes in password fields and when
    /// the history is switched off. The paste itself still went through, so the
    /// offer for that generation is withdrawn all the same.
    @Test func pasteWhileRecordingIsOffWithdrawsTheOfferButKeepsNothing() {
        let f = PasteFixture()
        f.allowed = false
        f.recorder.didPaste("mật khẩu", changeCount: 1)
        #expect(f.saved.isEmpty)
        #expect(f.updates == 0)
        #expect(f.recorder.lastPastedChangeCount == 1)
    }

    @Test func emptyTextAndEndedSessionsAreIgnored() {
        let f = PasteFixture()
        f.recorder.didPaste("", changeCount: 1)
        f.recorder.end()
        f.recorder.didPaste("muộn", changeCount: 1)
        #expect(f.saved.isEmpty)
        #expect(f.marks.pasted == nil)
    }

    @Test func failedWriteSendsNoUpdate() {
        let f = PasteFixture()
        f.writesSucceed = false
        f.recorder.didPaste("rơi", changeCount: 1)
        #expect(f.updates == 0)
        #expect(f.recorder.lastPastedChangeCount == 1)
    }
}

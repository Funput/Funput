import FunputShared
import Testing

@MainActor
struct ClipboardCaptureControllerTests {
    @Test func capturesBeforePasteWithoutChangingText() async {
        let f = CaptureFixture()
        await f.sync()
        #expect(f.saved.map(\.text) == ["  Tiếng Việt\n🙂  "])
        #expect(f.updates == 1)
        #expect(f.controller.lastPastedChangeCount == nil)
        for _ in 0..<1_000 { await f.sync() }
        #expect(f.gateway.reads == 1)
        #expect(f.saved.count == 1)
        f.gateway.copy("new")
        await f.sync()
        #expect(f.saved.count == 2)
    }

    @Test func copyDuringReadDiscardsUnstableResult() async {
        let f = CaptureFixture()
        f.gateway.duringRead = {
            f.gateway.duringRead = nil
            f.gateway.copy("new")
        }
        // The first result is thrown away; the queued re-read picks up the new copy.
        await f.sync()
        #expect(f.gateway.reads == 2)
        #expect(f.saved.map(\.text) == ["new"])
        #expect(f.saved.first?.sourceChangeCount == 2)
    }

    /// A paste through the chip lands while a slow read (a Mac clipboard still on
    /// its way) is pending. The read must not save the same clipboard again.
    @Test func pasteDuringReadIsNotSavedTwice() async {
        let f = CaptureFixture()
        f.gateway.duringRead = {
            f.controller.didPaste(f.gateway.text!, changeCount: 1)
        }
        await f.sync()
        #expect(f.saved.count == 1)
        #expect(f.controller.lastPastedChangeCount == 1)
    }

    @Test func captureDoesNotSuppressPasteOffer() async {
        let f = CaptureFixture()
        await f.sync()
        let context = ClipboardOfferPolicy.Context(
            editorMode: .text, hasToolbar: true, hasFullAccess: true
        )
        #expect(ClipboardOfferPolicy.offer(
            snapshot: f.gateway.metadata,
            lastPastedChangeCount: f.controller.lastPastedChangeCount, context: context
        ) != nil)
        f.controller.didPaste(f.gateway.text!, changeCount: 1)
        #expect(f.saved.count == 1)
        #expect(ClipboardOfferPolicy.offer(
            snapshot: f.gateway.metadata,
            lastPastedChangeCount: f.controller.lastPastedChangeCount, context: context
        ) == nil)
    }

    @Test func lateManualPasteDoesNotSuppressOrClaimNewClipboard() async {
        let f = CaptureFixture()
        f.gateway.copy("new")
        f.controller.didPaste("old", changeCount: 1)
        #expect(f.saved.first?.sourceChangeCount == -1)
        #expect(f.controller.lastPastedChangeCount == nil)
        await f.sync()
        #expect(f.saved.last?.text == "new")
    }

    @Test func ignoresEmptyTextAndImages() async {
        let f = CaptureFixture()
        f.gateway.copy("")
        await f.sync()
        f.gateway.copy(nil)
        await f.sync()
        #expect(f.saved.isEmpty)
        #expect(!f.controller.needsRetry)
    }

    @Test func ignoresURLShapedProvidersWithoutPlainText() async {
        let f = CaptureFixture()
        f.gateway.metadata = ClipboardSnapshot(
            changeCount: 2, hasStrings: true, hasURLs: true, hasPlainText: false
        )
        f.gateway.text = nil
        await f.sync()
        #expect(f.gateway.reads == 0)
        #expect(f.saved.isEmpty)
        #expect(!f.controller.needsRetry)
    }
}

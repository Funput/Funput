import FunputShared
import Testing

@MainActor
struct ClipboardCaptureControllerTests {
    @Test func capturesBeforePasteWithoutChangingText() {
        let f = CaptureFixture()
        f.controller.synchronize()
        #expect(f.saved.map(\.text) == ["  Tiếng Việt\n🙂  "])
        #expect(f.updates == 1)
        #expect(f.controller.lastPastedChangeCount == nil)
        for _ in 0..<1_000 { f.controller.synchronize() }
        #expect(f.gateway.reads == 1)
        #expect(f.saved.count == 1)
        f.gateway.copy("new")
        f.controller.synchronize()
        #expect(f.saved.count == 2)
    }

    @Test func copyDuringReadDiscardsUnstableResult() {
        let f = CaptureFixture()
        f.gateway.duringRead = { f.gateway.copy("new") }
        f.controller.synchronize()
        #expect(f.saved.isEmpty)
        f.gateway.duringRead = nil
        f.controller.synchronize()
        #expect(f.saved.first?.text == "new")
        #expect(f.saved.first?.sourceChangeCount == 2)
    }

    @Test func captureDoesNotSuppressPasteOffer() {
        let f = CaptureFixture()
        f.controller.synchronize()
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

    @Test func lateManualPasteDoesNotSuppressOrClaimNewClipboard() {
        let f = CaptureFixture()
        f.gateway.copy("new")
        f.controller.didPaste("old", changeCount: 1)
        #expect(f.saved.first?.sourceChangeCount == -1)
        #expect(f.controller.lastPastedChangeCount == nil)
        f.controller.synchronize()
        #expect(f.saved.last?.text == "new")
    }

    @Test func ignoresEmptyTextAndImages() {
        let f = CaptureFixture()
        f.gateway.copy("")
        f.controller.synchronize()
        f.gateway.copy(nil)
        f.controller.synchronize()
        #expect(f.saved.isEmpty)
        #expect(!f.controller.needsRetry)
    }
}

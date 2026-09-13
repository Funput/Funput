import FunputShared
import Testing

@MainActor
struct ClipboardCaptureLifecycleTests {
    @Test func deniedReadsPauseUntilExplicitRetryEvenAcrossNewCopies() {
        let f = CaptureFixture()
        f.gateway.text = nil
        f.controller.synchronize()
        #expect(f.controller.needsRetry)
        f.gateway.copy("allowed now")
        f.controller.synchronize()
        f.controller.end()
        f.controller.begin()
        f.controller.synchronize()
        #expect(f.gateway.reads == 1)
        #expect(f.saved.isEmpty)
        f.controller.synchronize(retry: true)
        #expect(f.saved.first?.text == "allowed now")
        #expect(!f.controller.needsRetry)
    }

    @Test func writeFailureIsNotMarkedCaptured() {
        let f = CaptureFixture()
        f.writesSucceed = false
        f.controller.synchronize()
        #expect(f.controller.needsRetry)
        f.writesSucceed = true
        f.controller.synchronize(retry: true)
        #expect(f.saved.count == 1)
    }

    @Test func lostAccessOrClosedSessionCannotSaveAnInFlightRead() {
        for close in [false, true] {
            let f = CaptureFixture()
            f.gateway.duringRead = {
                if close { f.controller.end() } else { f.allowed = false }
            }
            f.controller.synchronize()
            #expect(f.saved.isEmpty)
            f.controller.synchronize()
            #expect(f.gateway.reads == 1)
        }
    }

    @Test func clearingSuppressesSameClipboardUntilNextSession() {
        let f = CaptureFixture()
        f.controller.synchronize()
        f.controller.suppressCurrentAfterClear()
        f.saved = []
        f.controller.synchronize(retry: true)
        #expect(f.saved.isEmpty)
        f.controller.end()
        f.controller.begin()
        f.controller.synchronize()
        #expect(f.saved.count == 1)
    }

    /// Every pasteboard read shows the user a system banner, so reopening the
    /// keyboard on a clipboard the previous session already captured must read
    /// nothing at all.
    @Test func reopeningOnAnUnchangedClipboardReadsNothing() {
        let f = CaptureFixture()
        f.controller.synchronize()
        #expect(f.gateway.reads == 1)
        f.marks.captured = f.gateway.metadata.changeCount

        for _ in 0..<5 {
            f.controller.end()
            f.controller.begin()
            f.controller.synchronize()
        }
        #expect(f.gateway.reads == 1)
        #expect(f.saved.count == 1)

        // A copy made while the keyboard was away is still picked up on reopening.
        f.gateway.copy("trong luc dong")
        f.controller.end()
        f.controller.begin()
        f.controller.synchronize()
        #expect(f.gateway.reads == 2)
        #expect(f.saved.map(\.text) == ["  Tiếng Việt\n🙂  ", "trong luc dong"])
    }

    /// Switching apps rebuilds the keyboard. The offer must not come back with it.
    @Test func pasteSurvivesTheSessionAndOnlyANewCopyBringsTheOfferBack() {
        let f = CaptureFixture()
        f.controller.didPaste(f.gateway.text!, changeCount: 1)
        #expect(f.controller.lastPastedChangeCount == 1)

        f.controller.end()
        f.controller.begin()
        #expect(f.controller.lastPastedChangeCount == 1)

        f.gateway.copy("thu hai")
        f.controller.end()
        f.controller.begin()
        #expect(f.controller.lastPastedChangeCount == 1)
        #expect(ClipboardOfferPolicy.offer(
            snapshot: f.gateway.metadata,
            lastPastedChangeCount: f.controller.lastPastedChangeCount,
            context: ClipboardOfferPolicy.Context(
                editorMode: .text, hasToolbar: true, hasFullAccess: true
            )
        ) != nil)
    }

    @Test func reentrantNotificationDoesNotStartAnotherRead() {
        let f = CaptureFixture()
        f.gateway.duringRead = { f.controller.synchronize() }
        f.controller.synchronize()
        #expect(f.gateway.reads == 1)
        #expect(f.saved.count == 1)
    }
}

package app.funput.funput.ime.speech.editor

import app.funput.funput.ime.speech.session.SpeechEditorAnchor
import org.junit.Assert.*
import org.junit.Test

class SpeechAnchorResolverTest {
    @Test fun knownUnchangedCaretDoesNotRequireFreshCallbackOrExtractedText() {
        val f = AnchorFixture()
        f.prepare()
        assertEquals(listOf(f.tracker.anchor()), f.results)
        assertEquals(1, f.environment.finishes)
    }

    @Test fun synchronousFinishCallbackReconcilesTheNewCaretBeforeDelivery() {
        val f = AnchorFixture()
        f.environment.onFinish = { f.update(8) }
        f.prepare()
        assertEquals(8, f.results.single()!!.caret)
        assertTrue(f.tracker.matches(f.results.single()!!))
    }

    @Test fun outstandingEditWaitsForSelectionWithoutAcceptingAnOldSeed() {
        val f = AnchorFixture()
        f.tracker.invalidate(expectSelection = true)
        f.prepare()
        assertTrue(f.results.isEmpty())
        f.update(8)
        assertEquals(8, f.results.single()!!.caret)
    }

    @Test fun authoritativeExtractedSelectionResolvesAnOutstandingEdit() {
        val f = AnchorFixture()
        f.tracker.invalidate(expectSelection = true)
        f.environment.observed = SpeechSelection(9, 9)
        f.prepare()
        assertEquals(9, f.results.single()!!.caret)
    }

    @Test fun identicalEditorRestartDuringFinishIsRejected() {
        val f = AnchorFixture()
        f.environment.onFinish = { f.tracker.startInput(true, 3, 3) }
        f.prepare()
        assertEquals(listOf<SpeechEditorAnchor?>(null), f.results)
    }

    @Test fun externalEditDuringFinishIsRejectedEvenWithConfirmedCaret() {
        val f = AnchorFixture()
        f.environment.onFinish = { f.tracker.invalidate() }
        f.prepare()
        assertNull(f.results.single())
    }

    @Test fun closeSuppressesLateSelectionAndReplacementOwnsOnlyItsResult() {
        val f = AnchorFixture()
        f.tracker.invalidate(expectSelection = true)
        val old = f.prepare()
        old.cancel()
        f.update(8)
        assertTrue(f.results.isEmpty())
        f.prepare()
        old.cancel()
        assertEquals(8, f.results.single()!!.caret)
    }

    @Test fun nonCollapsedOrUnknownOrDisallowedCaretNeverFinishesComposition() {
        for ((allowed, start, end) in listOf(Triple(true, -1, -1), Triple(true, 2, 5), Triple(false, 3, 3))) {
            val f = AnchorFixture()
            f.tracker.startInput(allowed, start, end)
            f.prepare()
            assertNull(f.results.single())
            assertEquals(0, f.environment.finishes)
        }
    }

    @Test fun connectionFailureAndPermissionLossRejectPreparation() {
        val f = AnchorFixture()
        f.environment.onFinish = { f.environment.usable = false }
        f.prepare()
        assertNull(f.results.single())
        val failing = AnchorFixture()
        failing.environment.onFinish = { error("connection") }
        failing.prepare()
        assertNull(failing.results.single())
    }

    @Test fun reentrantSelectionReadCannotRecursivelyResolveOrDeliverTwice() {
        val f = AnchorFixture()
        f.environment.onRead = { f.update(4) }
        f.environment.observed = SpeechSelection(4, 4)
        f.prepare()
        assertEquals(1, f.results.size)
        assertEquals(4, f.results.single()!!.caret)
    }
}

private class AnchorFixture {
    val tracker = SpeechEditorTracker().apply { startInput(true, 3, 3) }
    val environment = AnchorEnvironment()
    val resolver = SpeechAnchorResolver(tracker, environment)
    val results = mutableListOf<SpeechEditorAnchor?>()
    fun prepare() = resolver.prepare { results += it }
    fun update(caret: Int) {
        tracker.selectionChanged(caret, caret)
        resolver.selectionUpdated()
    }
}

private class AnchorEnvironment : SpeechAnchorEnvironment {
    var usable = true
    var observed: SpeechSelection? = null
    var finishes = 0
    var onFinish: () -> Unit = {}
    var onRead: () -> Unit = {}
    override fun available() = usable
    override fun finishComposition() { finishes++; onFinish() }
    override fun selection(): SpeechSelection? { onRead(); return observed }
}

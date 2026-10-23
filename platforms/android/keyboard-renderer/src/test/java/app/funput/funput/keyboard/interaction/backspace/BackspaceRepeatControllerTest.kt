package app.funput.funput.keyboard.interaction.backspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceRepeatControllerTest {
    private val scheduler = TestScheduler()
    private val emittedStages = mutableListOf<BackspaceRepeatStage>()
    private val controller = BackspaceRepeatController(
        schedule = scheduler::schedule,
        cancel = scheduler::cancel,
        onRepeat = { emittedStages += it },
    )

    @Test
    fun quickTapLeavesReleaseActionUnsuppressed() {
        controller.update(pointerId = 3, isBackspace = true)

        assertFalse(controller.finish(pointerId = 3, isBackspace = true))
        assertTrue(emittedStages.isEmpty())
        assertNull(scheduler.task)
    }

    @Test
    fun holdRepeatsAndSuppressesExtraReleaseAction() {
        controller.update(pointerId = 3, isBackspace = true)
        assertEquals(400L, scheduler.delayMillis)

        scheduler.runNext()
        scheduler.runNext()

        assertEquals(
            listOf(BackspaceRepeatStage.Character, BackspaceRepeatStage.Character),
            emittedStages,
        )
        assertEquals(50L, scheduler.delayMillis)
        assertTrue(controller.finish(pointerId = 3, isBackspace = true))
        assertNull(scheduler.task)
    }

    @Test
    fun sustainedHoldEscalatesFromCharacterToWordDeletionAndResetsAfterRelease() {
        controller.update(pointerId = 3, isBackspace = true)

        repeat(BackspaceRepeatPolicy.WordStageStartIndex) {
            scheduler.runNext()
        }
        assertTrue(emittedStages.all { it == BackspaceRepeatStage.Character })

        scheduler.runNext()
        assertEquals(BackspaceRepeatStage.Word, emittedStages.last())
        assertEquals(BackspaceRepeatPolicy.WordIntervalMillis, scheduler.delayMillis)
        assertTrue(controller.finish(pointerId = 3, isBackspace = true))

        emittedStages.clear()
        controller.update(pointerId = 3, isBackspace = true)
        scheduler.runNext()
        assertEquals(listOf(BackspaceRepeatStage.Character), emittedStages)
    }

    @Test
    fun slidingAwayCancelsRepeatImmediately() {
        controller.update(pointerId = 3, isBackspace = true)

        controller.update(pointerId = 3, isBackspace = false)

        assertNull(scheduler.task)
        assertFalse(controller.finish(pointerId = 3, isBackspace = false))
    }

    @Test
    fun cancelClearsEveryPointer() {
        controller.update(pointerId = 3, isBackspace = true)
        controller.update(pointerId = 11, isBackspace = true)

        controller.cancelAll()

        assertNull(scheduler.task)
        assertFalse(controller.finish(pointerId = 3, isBackspace = true))
        assertFalse(controller.finish(pointerId = 11, isBackspace = true))
    }

    @Test
    fun nextHeldPointerTakesOverAfterActivePointerFinishes() {
        controller.update(pointerId = 3, isBackspace = true)
        controller.update(pointerId = 11, isBackspace = true)
        scheduler.runNext()

        assertTrue(controller.finish(pointerId = 3, isBackspace = true))
        assertEquals(400L, scheduler.delayMillis)

        scheduler.runNext()
        assertEquals(2, emittedStages.size)
        assertTrue(controller.finish(pointerId = 11, isBackspace = true))
    }

    private class TestScheduler {
        var task: Runnable? = null
        var delayMillis: Long? = null

        fun schedule(task: Runnable, delayMillis: Long) {
            this.task = task
            this.delayMillis = delayMillis
        }

        fun cancel(task: Runnable) {
            if (this.task === task) this.task = null
        }

        fun runNext() {
            val nextTask = task ?: error("No task scheduled")
            task = null
            nextTask.run()
        }
    }
}

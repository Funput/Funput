package app.funput.funput.keyboard.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceRepeatControllerTest {
    private val scheduler = TestScheduler()
    private var repeatCount = 0
    private val controller = BackspaceRepeatController(
        schedule = scheduler::schedule,
        cancel = scheduler::cancel,
        onRepeat = { repeatCount++ },
        initialDelayMillis = 400L,
        repeatIntervalMillis = 50L,
    )

    @Test
    fun quickTapLeavesReleaseActionUnsuppressed() {
        controller.update(pointerId = 3, isBackspace = true)

        assertFalse(controller.finish(pointerId = 3, isBackspace = true))
        assertEquals(0, repeatCount)
        assertNull(scheduler.task)
    }

    @Test
    fun holdRepeatsAndSuppressesExtraReleaseAction() {
        controller.update(pointerId = 3, isBackspace = true)
        assertEquals(400L, scheduler.delayMillis)

        scheduler.runNext()
        scheduler.runNext()

        assertEquals(2, repeatCount)
        assertEquals(50L, scheduler.delayMillis)
        assertTrue(controller.finish(pointerId = 3, isBackspace = true))
        assertNull(scheduler.task)
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
        assertEquals(2, repeatCount)
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

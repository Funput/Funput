package app.funput.funput.keyboard.interaction.gestures

/**
 * Fires once a single contact has stayed down past [delayMillis].
 *
 * Shared by the spacebar trackpad: arming is not claiming, so a resting thumb can
 * still lift and type a space.
 */
internal class KeyHoldController(
    private val schedule: (Runnable, Long) -> Unit,
    private val cancel: (Runnable) -> Unit,
    private val delayMillis: Long = DefaultDelayMillis,
    private val onActivate: (Int) -> Unit,
) {
    private val tasks = mutableMapOf<Int, Runnable>()

    init {
        require(delayMillis > 0L) { "Hold delay must be positive" }
    }

    fun start(pointerId: Int) {
        if (pointerId in tasks) return
        val task = Runnable {
            if (tasks.remove(pointerId) != null) onActivate(pointerId)
        }
        tasks[pointerId] = task
        schedule(task, delayMillis)
    }

    fun cancel(pointerId: Int) {
        tasks.remove(pointerId)?.let(cancel)
    }

    fun cancelAll() {
        tasks.values.forEach(cancel)
        tasks.clear()
    }

    private companion object {
        const val DefaultDelayMillis = 350L
    }
}

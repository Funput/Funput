package app.funput.funput.keyboard.interaction

/** Schedules Backspace repeats while preserving one action for a normal tap. */
internal class BackspaceRepeatController(
    private val schedule: (task: Runnable, delayMillis: Long) -> Unit,
    private val cancel: (task: Runnable) -> Unit,
    private val onRepeat: () -> Unit,
    private val initialDelayMillis: Long = InitialDelayMillis,
    private val repeatIntervalMillis: Long = RepeatIntervalMillis,
) {
    private val pointerIds = linkedSetOf<Int>()
    private var activePointerId: Int? = null
    var hasRepeated = false
        private set
    private val repeatTask = object : Runnable {
        override fun run() {
            if (activePointerId == null) return
            hasRepeated = true
            onRepeat()
            schedule(this, repeatIntervalMillis)
        }
    }

    init {
        require(initialDelayMillis > 0L) { "Initial repeat delay must be positive" }
        require(repeatIntervalMillis > 0L) { "Repeat interval must be positive" }
    }

    fun update(pointerId: Int, isBackspace: Boolean) {
        if (isBackspace) {
            pointerIds += pointerId
            if (activePointerId == null) start(pointerId)
        } else if (pointerIds.remove(pointerId) && activePointerId == pointerId) {
            stopActive()
            startNextPointer()
        }
    }

    /** Returns true when release should not emit an additional Backspace action. */
    fun finish(pointerId: Int, isBackspace: Boolean): Boolean {
        val suppressRelease = activePointerId == pointerId && isBackspace && hasRepeated
        pointerIds.remove(pointerId)
        if (activePointerId == pointerId) {
            stopActive()
            startNextPointer()
        }
        return suppressRelease
    }

    fun cancelAll() {
        pointerIds.clear()
        stopActive()
    }

    private fun start(pointerId: Int) {
        activePointerId = pointerId
        hasRepeated = false
        schedule(repeatTask, initialDelayMillis)
    }

    private fun stopActive() {
        if (activePointerId != null) cancel(repeatTask)
        activePointerId = null
        hasRepeated = false
    }

    private fun startNextPointer() {
        pointerIds.firstOrNull()?.let(::start)
    }

    private companion object {
        const val InitialDelayMillis = 400L
        const val RepeatIntervalMillis = 50L
    }
}

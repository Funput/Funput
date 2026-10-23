package app.funput.funput.keyboard.interaction.backspace

/** Schedules Backspace repeats while preserving one action for a normal tap. */
internal class BackspaceRepeatController(
    private val schedule: (task: Runnable, delayMillis: Long) -> Unit,
    private val cancel: (task: Runnable) -> Unit,
    private val onRepeat: (BackspaceRepeatStage) -> Unit,
    private val policy: BackspaceRepeatPolicy = BackspaceRepeatPolicy.Default,
) {
    private val pointerIds = linkedSetOf<Int>()
    private var activePointerId: Int? = null
    private var repeatIndex = 0
    var hasRepeated = false
        private set

    private val repeatTask = object : Runnable {
        override fun run() {
            if (activePointerId == null) return
            hasRepeated = true
            val step = policy.step(repeatIndex++)
            onRepeat(step.stage)
            schedule(this, step.nextDelayMillis)
        }
    }

    init {
        require(policy.initialDelayMillis > 0L) { "Initial repeat delay must be positive" }
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
        repeatIndex = 0
        hasRepeated = false
        schedule(repeatTask, policy.initialDelayMillis)
    }

    private fun stopActive() {
        if (activePointerId != null) cancel(repeatTask)
        activePointerId = null
        repeatIndex = 0
        hasRepeated = false
    }

    private fun startNextPointer() {
        pointerIds.firstOrNull()?.let(::start)
    }
}

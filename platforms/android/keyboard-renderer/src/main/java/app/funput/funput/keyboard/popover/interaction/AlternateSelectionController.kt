package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.popover.model.KeyAlternate
import app.funput.funput.keyboard.popover.rendering.AlternatePaletteLayout

internal class AlternateSelectionController(
    private val keyBounds: (String) -> KeyBounds?,
    private val surfaceBounds: () -> KeyBounds,
    private val schedule: (Runnable, Long) -> Unit,
    private val cancel: (Runnable) -> Unit,
    private val touchSlop: Float,
    private val density: Float,
    private val onCaptured: (Int) -> Unit,
    private val onFeedback: () -> Unit,
    private val onChanged: () -> Unit,
    private val onSelected: (KeySpec, KeyAlternate) -> Unit,
) {
    private data class Session(
        val key: KeySpec,
        val startX: Float,
        val startY: Float,
        val task: Runnable,
        var layout: AlternatePaletteLayout? = null,
        var selectedIndex: Int? = null,
        var activationOrder: Long = 0,
    )

    private val sessions = mutableMapOf<Int, Session>()
    private var nextActivationOrder = 1L

    val preview: AlternateSelectionPreview?
        get() = sessions.values.filter { it.layout != null }.maxByOrNull { it.activationOrder }?.let {
            AlternateSelectionPreview(it.key, requireNotNull(it.layout), it.selectedIndex)
        }

    fun isCaptured(pointerId: Int): Boolean = sessions[pointerId]?.layout != null

    fun start(pointerId: Int, key: KeySpec?, x: Float, y: Float) {
        if (key == null || key.alternates.isEmpty()) return
        val task = Runnable { activate(pointerId) }
        sessions[pointerId] = Session(key, x, y, task)
        schedule(task, HoldDelayMillis)
    }

    fun move(pointerId: Int, keyId: String?, x: Float, y: Float) {
        val session = sessions[pointerId] ?: return
        val layout = session.layout
        if (layout == null) {
            val dx = x - session.startX
            val dy = y - session.startY
            if (keyId != session.key.id || dx * dx + dy * dy > touchSlop * touchSlop) {
                remove(pointerId)
            }
            return
        }
        val next = layout.indexAt(x, y, density)
        if (next != session.selectedIndex) {
            session.selectedIndex = next
            onFeedback()
            onChanged()
        }
    }

    fun finish(pointerId: Int, x: Float, y: Float): Boolean {
        val session = sessions.remove(pointerId) ?: return false
        cancel(session.task)
        val layout = session.layout ?: return false
        val index = layout.indexAt(x, y, density)
        index?.let { session.key.alternates.getOrNull(it) }?.let {
            onSelected(session.key, it)
        }
        onChanged()
        return true
    }

    fun cancelAll() {
        sessions.values.forEach { cancel(it.task) }
        val changed = sessions.values.any { it.layout != null }
        sessions.clear()
        if (changed) onChanged()
    }

    private fun activate(pointerId: Int) {
        val session = sessions[pointerId] ?: return
        val source = keyBounds(session.key.id) ?: return remove(pointerId)
        session.layout = AlternatePaletteLayout.resolve(
            session.key.alternates.size,
            source,
            surfaceBounds(),
            density,
        )
        session.selectedIndex = 0
        session.activationOrder = nextActivationOrder++
        onCaptured(pointerId)
        onFeedback()
        onChanged()
    }

    private fun remove(pointerId: Int) {
        sessions.remove(pointerId)?.let { cancel(it.task) }
    }

    private companion object {
        const val HoldDelayMillis = 350L
    }
}

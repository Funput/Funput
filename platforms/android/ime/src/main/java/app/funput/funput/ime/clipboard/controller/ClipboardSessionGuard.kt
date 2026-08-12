package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.keyboard.model.KeyboardEditorMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ClipboardSessionGuard {
    private var active = false
    private var mode = KeyboardEditorMode.TEXT
    private var generation = 0L

    val token: Long get() = generation
    val isActive: Boolean get() = active
    val editorMode: KeyboardEditorMode get() = mode

    fun start(editorMode: KeyboardEditorMode) {
        active = true
        mode = editorMode
        invalidate()
    }

    fun stop() {
        if (active) {
            active = false
            invalidate()
        }
    }

    fun invalidate() { generation += 1 }

    fun matches(value: Long) = active && value == generation

    fun allows(enabled: Boolean) = enabled && active && !mode.isPassword && !mode.usesKeypad
}

internal class ClipboardObservationSlot {
    private var observation: ClipboardObservation? = null
    private var key: Any? = null

    fun open(
        scope: CoroutineScope,
        observe: (() -> Unit) -> ClipboardObservation,
        isValid: () -> Boolean,
        onChanged: () -> Unit,
    ) {
        if (observation != null) return
        val currentKey = Any()
        key = currentKey
        observation = observe {
            scope.launch {
                if (key === currentKey && isValid()) onChanged()
            }
        }
    }

    fun close() {
        key = null
        observation?.close()
        observation = null
    }
}

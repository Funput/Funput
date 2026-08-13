package app.funput.funput.keyboard.ui.panel

import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.ui.KeyboardPanel
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry

internal class KeyboardClipboardPanelState(
    private val surface: KeyboardSurfaceView,
    private val editorMode: () -> KeyboardEditorMode,
    private val activePanel: () -> KeyboardPanel,
    private val showLetters: () -> Unit,
) {
    private var panel: ClipboardPanelView? = null
    var enabled: Boolean = false
        set(value) {
            field = value
            syncAvailability()
            if (!value && activePanel() == KeyboardPanel.CLIPBOARD) showLetters()
        }
    var entries: List<KeyboardClipboardEntry> = emptyList()
        set(value) { field = value; syncPanel() }
    var loading: Boolean = false
        set(value) { field = value; syncPanel() }

    fun attach(value: ClipboardPanelView) {
        panel = value
        syncPanel()
    }

    fun editorModeChanged() = syncAvailability()

    fun available(): Boolean = enabled && !editorMode().isPassword && !editorMode().usesKeypad

    private fun syncAvailability() { surface.clipboardKeyVisible = available() }
    private fun syncPanel() { panel?.submit(entries, loading) }
}

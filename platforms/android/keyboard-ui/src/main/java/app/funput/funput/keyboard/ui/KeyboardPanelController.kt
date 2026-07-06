package app.funput.funput.keyboard.ui

internal class KeyboardPanelController {
    var activePanel: KeyboardPanel = KeyboardPanel.LETTERS
        private set

    fun show(panel: KeyboardPanel): Boolean {
        if (panel == activePanel) return false
        activePanel = panel
        return true
    }
}

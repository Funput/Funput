package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardLayoutMode

/** Routes renderer actions that change the visible keyboard panel. */
internal fun FunputKeyboardView.routeKeyAction(action: KeyAction) = when (action) {
    KeyAction.Symbols -> showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_PRIMARY)
    KeyAction.MoreSymbols -> showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_SECONDARY)
    KeyAction.Letters -> showLettersPanel()
    KeyAction.SwitchInputMethod -> callbacks.dispatchInputMethodSwitchRequest()
    else -> callbacks.dispatch(action)
}

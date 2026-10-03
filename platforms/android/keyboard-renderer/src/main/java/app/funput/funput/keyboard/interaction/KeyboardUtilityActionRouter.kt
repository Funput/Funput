package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.utility.KeyboardUtilityAction
import app.funput.funput.keyboard.utility.KeyboardUtilityDispatcher
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.SuggestionSelection

/** Routes toolbar utilities without adding their policies to pointer handling. */
internal class KeyboardUtilityActionRouter(
    private val onSuggestionSelected: (SuggestionSelection) -> Unit,
    private val utilities: KeyboardUtilityDispatcher,
) {
    fun dispatch(
        keyId: String?,
        key: KeySpec?,
        selection: SuggestionSelection?,
        onKeyboardKey: (String) -> Unit,
    ) {
        when {
            selection != null -> onSuggestionSelected(selection)
            keyId == ClipboardTargetId -> utilities.dispatch(KeyboardUtilityAction.CLIPBOARD_PASTE)
            key?.role == KeyRole.PLACEMENT -> utilities.dispatch(KeyboardUtilityAction.PLACEMENT)
            key?.role == KeyRole.CLIPBOARD -> utilities.dispatch(KeyboardUtilityAction.CLIPBOARD_PANEL)
            key?.role == KeyRole.MICROPHONE -> utilities.dispatch(KeyboardUtilityAction.MICROPHONE)
            key?.role == KeyRole.EMOJI -> utilities.dispatch(KeyboardUtilityAction.EMOJI)
            keyId != null -> onKeyboardKey(keyId)
        }
    }
}

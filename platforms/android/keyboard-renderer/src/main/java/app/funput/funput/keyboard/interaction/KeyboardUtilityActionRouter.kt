package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.SuggestionSelection

/** Routes toolbar utilities without adding their policies to pointer handling. */
internal class KeyboardUtilityActionRouter(
    private val onSuggestionSelected: (SuggestionSelection) -> Unit,
    private val onEmojiRequested: () -> Unit,
    private val onPlacementEditorRequested: () -> Unit,
    private val onClipboardPanelRequested: () -> Unit,
    private val onClipboardRequested: () -> Unit,
) {
    fun dispatch(
        keyId: String?,
        key: KeySpec?,
        selection: SuggestionSelection?,
        onKeyboardKey: (String) -> Unit,
    ) {
        when {
            selection != null -> onSuggestionSelected(selection)
            keyId == ClipboardTargetId -> onClipboardRequested()
            key?.role == KeyRole.PLACEMENT -> onPlacementEditorRequested()
            key?.role == KeyRole.CLIPBOARD -> onClipboardPanelRequested()
            key?.role == KeyRole.EMOJI -> onEmojiRequested()
            keyId != null -> onKeyboardKey(keyId)
        }
    }
}

package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.popover.model.KeyAlternate

/** Function choices never travel through the text-input dispatcher. */
internal class AlternateActionRouter(
    private val dispatcher: KeyboardActionDispatcher,
    private val cancelInteraction: () -> Unit,
    private val openPlacement: () -> Unit,
    private val openSettings: () -> Unit,
) {
    fun dispatch(key: KeySpec, alternate: KeyAlternate) {
        when (alternate) {
            is KeyAlternate.Text -> dispatcher.dispatchAlternate(key, alternate)
            is KeyAlternate.Action -> {
                cancelInteraction()
                when (alternate) {
                    KeyAlternate.Action.PLACEMENT -> openPlacement()
                    KeyAlternate.Action.SETTINGS -> openSettings()
                }
            }
        }
    }
}

package app.funput.funput.keyboard.popover.accessibility

import app.funput.funput.keyboard.popover.model.KeyAlternate
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState

internal data class AlternateAccessibilityAction(
    val index: Int,
    val actionId: Int,
    val label: String,
)

internal fun KeySpec.alternateAccessibilityActions(
    shiftState: ShiftState,
): List<AlternateAccessibilityAction> = alternates.mapIndexed { index, alternate ->
    val spokenLabel = if (alternate is KeyAlternate.Text && shiftState.isActive && alternate.shiftedText != alternate.text) {
        alternate.textFor(shiftState)
    } else {
        alternate.accessibilityLabel
    }
    AlternateAccessibilityAction(
        index = index,
        actionId = AlternateActionIdBase + index,
        label = if (alternate is KeyAlternate.Action) spokenLabel else "Chọn $spokenLabel",
    )
}

private const val AlternateActionIdBase = 0x01020000

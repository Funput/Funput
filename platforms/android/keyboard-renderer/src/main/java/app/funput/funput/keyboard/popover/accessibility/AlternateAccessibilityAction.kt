package app.funput.funput.keyboard.popover.accessibility

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
    AlternateAccessibilityAction(
        index = index,
        actionId = AlternateActionIdBase + index,
        label = "Chọn ${alternate.textFor(shiftState)}",
    )
}

private const val AlternateActionIdBase = 0x01020000

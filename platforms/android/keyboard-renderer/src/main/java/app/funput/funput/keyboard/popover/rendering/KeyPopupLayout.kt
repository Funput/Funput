package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.ShiftState

/** Pure popup policy and geometry, kept separate from Canvas drawing for testability. */
internal object KeyPopupLayout {
    fun isEligible(key: ResolvedKey, secure: Boolean): Boolean =
        !secure && key.spec.label.isNotEmpty() && key.spec.role in PopupRoles

    fun label(key: ResolvedKey, shiftState: ShiftState): String =
        if (key.spec.role == KeyRole.CHARACTER && shiftState.isActive) {
            key.spec.shiftedLabel ?: key.spec.label
        } else {
            key.spec.label
        }

    fun bounds(
        key: ResolvedKey,
        surfaceWidth: Float,
        popupWidth: Float,
        popupHeight: Float,
        edgeInset: Float,
        anchorOverlap: Float,
    ): KeyBounds {
        val width = popupWidth.coerceAtMost(surfaceWidth - edgeInset * 2f)
        val left = (key.bounds.centerX - width / 2f)
            .coerceIn(edgeInset, surfaceWidth - edgeInset - width)
        val bottom = maxOf(key.bounds.top + anchorOverlap, edgeInset + popupHeight)
        return KeyBounds(left, bottom - popupHeight, left + width, bottom)
    }

    private val PopupRoles = setOf(
        KeyRole.CHARACTER,
        KeyRole.VNI_MODIFIER,
        KeyRole.PUNCTUATION,
    )
}

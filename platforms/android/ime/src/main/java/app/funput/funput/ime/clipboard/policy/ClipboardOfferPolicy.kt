package app.funput.funput.ime.clipboard.policy

import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.keyboard.model.KeyboardEditorMode

internal object ClipboardOfferPolicy {
    data class Context(
        val enabled: Boolean,
        val active: Boolean,
        val editorMode: KeyboardEditorMode,
    )

    fun offer(
        snapshot: ClipboardSnapshot?,
        lastCapturedSourceToken: String?,
        context: Context,
    ): ClipboardOffer? {
        if (!allowsClipboard(context)) return null
        val value = snapshot ?: return null
        val kind = value.kind ?: return null
        if (value.sourceToken != null && value.sourceToken == lastCapturedSourceToken) return null
        return ClipboardOffer(
            kind = when {
                value.isSensitive -> ClipboardOfferKind.SENSITIVE
                kind == ClipboardContentKind.LINK -> ClipboardOfferKind.LINK
                else -> ClipboardOfferKind.TEXT
            },
            sourceToken = value.sourceToken,
        )
    }

    fun allowsClipboard(context: Context): Boolean =
        context.enabled && context.active && hasUtilityToolbar(context.editorMode)

    private fun hasUtilityToolbar(mode: KeyboardEditorMode): Boolean =
        !mode.isPassword && !mode.usesKeypad
}

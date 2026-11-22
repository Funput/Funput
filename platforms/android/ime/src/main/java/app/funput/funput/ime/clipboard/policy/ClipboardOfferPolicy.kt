package app.funput.funput.ime.clipboard.policy

import app.funput.funput.ime.clipboard.model.ClipboardOfferSource
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.keyboard.model.KeyboardEditorMode

internal object ClipboardOfferPolicy {
    /** @property showsPreview False when the user keeps Funput from recording what they copy. */
    data class Context(
        val enabled: Boolean,
        val active: Boolean,
        val editorMode: KeyboardEditorMode,
        val showsPreview: Boolean = true,
    )

    fun offer(
        snapshot: ClipboardSnapshot?,
        source: ClipboardOfferSource,
        context: Context,
    ): ClipboardOffer? {
        if (!allowsClipboard(context)) return null
        val value = snapshot ?: return null
        val kind = value.kind ?: return null
        if (value.sourceToken != null && value.sourceToken == source.lastPastedSourceToken) return null
        val previewable = context.showsPreview && !value.isSensitive
        return ClipboardOffer(
            kind = when {
                value.isSensitive -> ClipboardOfferKind.SENSITIVE
                kind == ClipboardContentKind.LINK -> ClipboardOfferKind.LINK
                else -> ClipboardOfferKind.TEXT
            },
            sourceToken = value.sourceToken,
            contentKind = value.kind,
            preview = source.heldText?.takeIf { previewable }?.let(ClipboardOfferPreview::of),
        )
    }

    fun allowsClipboard(context: Context): Boolean =
        context.enabled && context.active && hasUtilityToolbar(context.editorMode)

    private fun hasUtilityToolbar(mode: KeyboardEditorMode): Boolean =
        !mode.isPassword && !mode.usesKeypad
}

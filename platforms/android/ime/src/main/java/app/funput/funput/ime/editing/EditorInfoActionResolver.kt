package app.funput.funput.ime.editing

import android.view.inputmethod.EditorInfo
import app.funput.funput.keyboard.model.KeyboardEnterAction

/** Resolves Android editor metadata into Funput's platform-neutral action key. */
internal object EditorInfoActionResolver {
    fun resolve(info: EditorInfo): ImeEditorAction = resolve(
        imeOptions = info.imeOptions,
        actionLabel = info.actionLabel,
        actionId = info.actionId,
    )

    internal fun resolve(
        imeOptions: Int,
        actionLabel: CharSequence? = null,
        actionId: Int = 0,
    ): ImeEditorAction {
        if (imeOptions has EditorInfo.IME_FLAG_NO_ENTER_ACTION) return ImeEditorAction.NewLine

        actionLabel?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let { label ->
            return perform(KeyboardEnterAction.Custom(label), actionId)
        }

        return when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> perform(KeyboardEnterAction.Standard.GO, action)
            EditorInfo.IME_ACTION_SEARCH -> perform(KeyboardEnterAction.Standard.SEARCH, action)
            EditorInfo.IME_ACTION_SEND -> perform(KeyboardEnterAction.Standard.SEND, action)
            EditorInfo.IME_ACTION_NEXT -> perform(KeyboardEnterAction.Standard.NEXT, action)
            EditorInfo.IME_ACTION_DONE -> perform(KeyboardEnterAction.Standard.DONE, action)
            EditorInfo.IME_ACTION_PREVIOUS -> perform(KeyboardEnterAction.Standard.PREVIOUS, action)
            else -> ImeEditorAction.NewLine
        }
    }

    private fun perform(
        presentation: KeyboardEnterAction,
        actionId: Int,
    ) = ImeEditorAction(presentation, ImeEditCommand.PerformEditorAction(actionId))

    private infix fun Int.has(flag: Int): Boolean = this and flag != 0
}

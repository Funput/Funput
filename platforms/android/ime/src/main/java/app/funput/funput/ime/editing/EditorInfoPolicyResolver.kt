package app.funput.funput.ime.editing

import android.text.InputType
import android.text.TextUtils
import android.view.inputmethod.EditorInfo

/** Converts Android editor flags into one immutable runtime policy. */
internal object EditorInfoPolicyResolver {
    fun resolve(info: EditorInfo): EditorInfoPolicy = resolve(
        inputType = info.inputType,
        imeOptions = info.imeOptions,
        actionLabel = info.actionLabel,
        actionId = info.actionId,
    )

    internal fun resolve(
        inputType: Int,
        imeOptions: Int,
        actionLabel: CharSequence? = null,
        actionId: Int = 0,
    ): EditorInfoPolicy {
        val editorMode = EditorInfoKeyboardModeResolver.resolve(inputType)
        val isText = inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT
        return EditorInfoPolicy(
            editorMode = editorMode,
            editorAction = EditorInfoActionResolver.resolve(imeOptions, actionLabel, actionId),
            capitalizationModes = capitalizationModes(inputType, isText, editorMode.isPassword),
            isMultiline = isText && inputType has InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            suggestionSource = suggestionSource(inputType, isText, editorMode.isPassword),
            allowsPersonalizedLearning = !editorMode.isPassword &&
                !(imeOptions has EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING),
        )
    }

    private fun capitalizationModes(inputType: Int, isText: Boolean, isPassword: Boolean): Int {
        if (!isText || isPassword) return 0
        var modes = 0
        if (inputType has InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) modes = modes or TextUtils.CAP_MODE_CHARACTERS
        if (inputType has InputType.TYPE_TEXT_FLAG_CAP_WORDS) modes = modes or TextUtils.CAP_MODE_WORDS
        if (inputType has InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) modes = modes or TextUtils.CAP_MODE_SENTENCES
        return modes
    }

    private fun suggestionSource(
        inputType: Int,
        isText: Boolean,
        isPassword: Boolean,
    ): ImeSuggestionSource = when {
        !isText || isPassword -> ImeSuggestionSource.NONE
        inputType has InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS -> ImeSuggestionSource.NONE
        inputType has InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE -> ImeSuggestionSource.EDITOR
        else -> ImeSuggestionSource.FUNPUT
    }

    private infix fun Int.has(flag: Int): Boolean = this and flag != 0
}

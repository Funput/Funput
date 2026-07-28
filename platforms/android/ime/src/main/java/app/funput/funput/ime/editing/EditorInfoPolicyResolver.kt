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
        packageName = info.packageName,
    )

    internal fun resolve(
        inputType: Int,
        imeOptions: Int,
        actionLabel: CharSequence? = null,
        actionId: Int = 0,
        packageName: String? = null,
    ): EditorInfoPolicy {
        val editorMode = EditorInfoKeyboardModeResolver.resolve(inputType)
        val isText = inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT
        val source = suggestionSource(inputType, isText, editorMode.isPassword)
        val learningAllowed = !editorMode.isPassword &&
            !(imeOptions has EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        return EditorInfoPolicy(
            editorMode = editorMode,
            editorAction = EditorInfoActionResolver.resolve(imeOptions, actionLabel, actionId),
            capitalizationModes = capitalizationModes(inputType, isText, editorMode.isPassword),
            isMultiline = isText && inputType has InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            suggestionSource = source,
            allowsPersonalizedLearning = learningAllowed,
            allowsPersonalSuggestions = source == ImeSuggestionSource.FUNPUT &&
                learningAllowed && !isUri(inputType) && !isEmail(inputType),
            compositionRenderMode = CompositionCompatibilityPolicy.renderMode(packageName),
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

    private fun isUri(inputType: Int): Boolean =
        inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_TEXT_VARIATION_URI

    private fun isEmail(inputType: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    private infix fun Int.has(flag: Int): Boolean = this and flag != 0
}

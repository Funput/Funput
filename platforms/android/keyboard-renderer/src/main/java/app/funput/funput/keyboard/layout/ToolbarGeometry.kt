package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardLayout

/** Places optional toolbar actions before the higher-priority Settings and Emoji keys. */
internal object ToolbarGeometry {
    fun resolve(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
    ): ResolvedSuggestionBar? {
        val bar = layout.suggestionBar ?: return null
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val right = width - spec.horizontalPadding
        val emoji = boundsBefore(right, top, bottom, spec, separated = false)
        val settings = boundsBefore(emoji.left, top, bottom, spec)
        val system = bar.systemInputMethodKey?.let { boundsBefore(settings.left, top, bottom, spec) }
        val controlsLeft = system?.left ?: settings.left
        val logoSize = spec.suggestionBarHeight * ToolbarBrandMetrics.LogoSizeRatio
        val logoTop = top + (spec.suggestionBarHeight - logoSize) / 2f
        val logo = KeyBounds(spec.horizontalPadding, logoTop, spec.horizontalPadding + logoSize, logoTop + logoSize)
        val suggestionsLeft = logo.right + spec.horizontalGap
        val suggestionsRight = controlsLeft - if (bar.suggestionsEnabled) spec.horizontalGap else 0f
        val resolved = ResolvedSuggestionBar(
            bounds = KeyBounds(spec.horizontalPadding, top, right, bottom),
            logoBounds = logo,
            suggestionsBounds = KeyBounds(suggestionsLeft, top, suggestionsRight, bottom),
            systemInputMethodKey = system?.let { ResolvedKey(requireNotNull(bar.systemInputMethodKey), it) },
            settingsKey = ResolvedKey(bar.settingsKey, settings),
            emojiKey = ResolvedKey(bar.emojiKey, emoji),
            suggestionsEnabled = bar.suggestionsEnabled,
        )
        require(!resolved.suggestionsEnabled || resolved.suggestionsBounds.width > 0f) {
            "Keyboard is too narrow for the suggestion bar"
        }
        return resolved
    }

    private fun boundsBefore(
        right: Float,
        top: Float,
        bottom: Float,
        spec: KeyboardGeometrySpec,
        separated: Boolean = true,
    ): KeyBounds {
        val keyRight = right - if (separated) spec.horizontalGap else 0f
        return KeyBounds(keyRight - spec.suggestionBarHeight, top, keyRight, bottom)
    }
}

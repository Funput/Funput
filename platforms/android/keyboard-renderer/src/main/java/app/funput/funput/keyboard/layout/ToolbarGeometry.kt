package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardLayout

/** Places optional toolbar actions before the higher-priority Emoji key. */
internal object ToolbarGeometry {
    /**
     * Resolves toolbar bounds.
     *
     * When [showSettings] is false (suggestions present), Settings yields so the suggestion
     * region can extend to the emoji key — matching iOS utility-key arbitration.
     */
    fun resolve(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
        showSettings: Boolean = true,
        showClipboard: Boolean = false,
    ): ResolvedSuggestionBar? {
        val bar = layout.suggestionBar ?: return null
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val right = width - spec.horizontalPadding
        val emoji = boundsBefore(right, top, bottom, spec, separated = false)
        val clipboard = if (showClipboard && bar.clipboardKey != null) {
            boundsBefore(emoji.left, top, bottom, spec)
        } else {
            null
        }
        val settingsAnchor = clipboard?.left ?: emoji.left
        val settings = if (showSettings) boundsBefore(settingsAnchor, top, bottom, spec) else null
        val systemAnchor = settings?.left ?: clipboard?.left ?: emoji.left
        val system = bar.systemInputMethodKey?.let { boundsBefore(systemAnchor, top, bottom, spec) }
        val controlsLeft = system?.left ?: settings?.left ?: clipboard?.left ?: emoji.left
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
            settingsKey = settings?.let { ResolvedKey(bar.settingsKey, it) },
            clipboardKey = clipboard?.let { ResolvedKey(requireNotNull(bar.clipboardKey), it) },
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

package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardLayout

/** Places optional toolbar actions before the higher-priority Emoji key. */
internal object ToolbarGeometry {
    /**
     * Resolves toolbar bounds.
     *
     * Clipboard and placement yield when suggestions are present so candidates can use their room.
     */
    fun resolve(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
        showClipboard: Boolean = false,
        showPlacement: Boolean = true,
    ): ResolvedSuggestionBar? {
        val bar = layout.suggestionBar ?: return null
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val right = width - spec.horizontalPadding
        val emoji = boundsBefore(right, top, bottom, spec, separated = false)
        val placement = if (showPlacement) boundsBefore(emoji.left, top, bottom, spec) else null
        val clipboard = if (showClipboard && bar.clipboardKey != null) {
            boundsBefore(placement?.left ?: emoji.left, top, bottom, spec)
        } else {
            null
        }
        val systemAnchor = clipboard?.left ?: placement?.left ?: emoji.left
        val system = bar.systemInputMethodKey?.let { boundsBefore(systemAnchor, top, bottom, spec) }
        val controlsLeft = system?.left ?: clipboard?.left ?: placement?.left ?: emoji.left
        // Suggestions start at the band's leading edge, flush with the first key of the
        // rows below — nothing sits to their left.
        val suggestionsLeft = spec.horizontalPadding
        val suggestionsRight = controlsLeft - if (bar.suggestionsEnabled) spec.horizontalGap else 0f
        val resolved = ResolvedSuggestionBar(
            bounds = KeyBounds(spec.horizontalPadding, top, right, bottom),
            suggestionsBounds = KeyBounds(suggestionsLeft, top, suggestionsRight, bottom),
            systemInputMethodKey = system?.let { ResolvedKey(requireNotNull(bar.systemInputMethodKey), it) },
            clipboardKey = clipboard?.let { ResolvedKey(requireNotNull(bar.clipboardKey), it) },
            placementKey = placement?.let { ResolvedKey(bar.placementKey, it) },
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

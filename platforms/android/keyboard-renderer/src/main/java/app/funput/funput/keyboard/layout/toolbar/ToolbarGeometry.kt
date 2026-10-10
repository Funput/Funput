package app.funput.funput.keyboard.layout.toolbar

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState

/** Emoji and microphone reserve their space before optional utilities and candidates. */
internal object ToolbarGeometry {
    fun resolve(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
        showClipboard: Boolean = false,
        showPlacement: Boolean = true,
        microphone: KeyboardMicrophoneState = KeyboardMicrophoneState(),
    ): ResolvedSuggestionBar? {
        val bar = layout.suggestionBar ?: return null
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val right = width - spec.horizontalPadding
        val emoji = boundsBefore(right, top, bottom, spec, separated = false)
        val density = spec.suggestionBarHeight / spec.heightScale / ToolbarMetrics.SuggestionBarHeightDp
        val micWidth = density * ToolbarMetrics.MicrophoneWidthDp
        // Left side, Gboard order (left to right): keyboard mode, then voice input.
        var leftAnchor = spec.horizontalPadding
        fun leftKey(visible: Boolean, keyWidth: Float): KeyBounds? {
            if (!visible) return null
            val bounds = KeyBounds(leftAnchor, top, leftAnchor + keyWidth, bottom)
            leftAnchor = bounds.right + spec.horizontalGap
            return bounds
        }
        val placement = leftKey(showPlacement, spec.suggestionBarHeight)
        val mic = leftKey(microphone.visible && bar.microphoneKey != null, micWidth)
        require(emoji.left >= leftAnchor - spec.horizontalGap) {
            "Keyboard is too narrow for its required toolbar actions"
        }
        // Right side, right to left: emoji (fixed), then optional clipboard and system keys.
        var anchor = emoji.left
        fun optional(visible: Boolean): KeyBounds? {
            if (!visible) return null
            val bounds = boundsBefore(anchor, top, bottom, spec)
            if (bounds.left < leftAnchor + spec.horizontalGap) return null
            anchor = bounds.left
            return bounds
        }
        val clipboard = optional(showClipboard && bar.clipboardKey != null)
        val system = optional(bar.systemInputMethodKey != null)
        val suggestionsLeft = leftAnchor.coerceAtMost(anchor)
        val suggestionsRight = (anchor - if (bar.suggestionsEnabled) spec.horizontalGap else 0f)
            .coerceAtLeast(suggestionsLeft)
        return ResolvedSuggestionBar(
            bounds = KeyBounds(spec.horizontalPadding, top, right, bottom),
            suggestionsBounds = KeyBounds(suggestionsLeft, top, suggestionsRight, bottom),
            systemInputMethodKey = system?.let { ResolvedKey(requireNotNull(bar.systemInputMethodKey), it) },
            clipboardKey = clipboard?.let { ResolvedKey(requireNotNull(bar.clipboardKey), it) },
            placementKey = placement?.let { ResolvedKey(bar.placementKey, it) },
            emojiKey = ResolvedKey(bar.emojiKey, emoji),
            microphoneKey = mic?.let {
                ResolvedKey(requireNotNull(bar.microphoneKey).copy(accessibilityLabel = microphone.accessibilityLabel),
                    it, active = microphone.active)
            },
            suggestionsEnabled = bar.suggestionsEnabled,
            clipboardHintFits = suggestionsRight - suggestionsLeft >= density * ToolbarMetrics.ClipboardPasteWidthDp,
        )
    }

    private fun boundsBefore(
        right: Float,
        top: Float,
        bottom: Float,
        spec: KeyboardGeometrySpec,
        separated: Boolean = true,
        keyWidth: Float = spec.suggestionBarHeight,
    ): KeyBounds {
        val keyRight = right - if (separated) spec.horizontalGap else 0f
        return KeyBounds(keyRight - keyWidth, top, keyRight, bottom)
    }
}

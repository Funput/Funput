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
        val mic = if (microphone.visible && bar.microphoneKey != null) {
            boundsBefore(emoji.left, top, bottom, spec, keyWidth = micWidth)
        } else null
        require((mic?.left ?: emoji.left) >= spec.horizontalPadding) {
            "Keyboard is too narrow for its required toolbar actions"
        }
        var anchor = mic?.left ?: emoji.left
        fun optional(visible: Boolean): KeyBounds? {
            if (!visible) return null
            val bounds = boundsBefore(anchor, top, bottom, spec)
            if (bounds.left < spec.horizontalPadding + spec.horizontalGap) return null
            anchor = bounds.left
            return bounds
        }
        val placement = optional(showPlacement)
        val clipboard = optional(showClipboard && bar.clipboardKey != null)
        val system = optional(bar.systemInputMethodKey != null)
        val suggestionsRight = (anchor - if (bar.suggestionsEnabled) spec.horizontalGap else 0f)
            .coerceAtLeast(spec.horizontalPadding)
        return ResolvedSuggestionBar(
            bounds = KeyBounds(spec.horizontalPadding, top, right, bottom),
            suggestionsBounds = KeyBounds(spec.horizontalPadding, top, suggestionsRight, bottom),
            systemInputMethodKey = system?.let { ResolvedKey(requireNotNull(bar.systemInputMethodKey), it) },
            clipboardKey = clipboard?.let { ResolvedKey(requireNotNull(bar.clipboardKey), it) },
            placementKey = placement?.let { ResolvedKey(bar.placementKey, it) },
            emojiKey = ResolvedKey(bar.emojiKey, emoji),
            microphoneKey = mic?.let {
                ResolvedKey(requireNotNull(bar.microphoneKey).copy(accessibilityLabel = microphone.accessibilityLabel),
                    it, active = microphone.active)
            },
            suggestionsEnabled = bar.suggestionsEnabled,
            clipboardHintFits = suggestionsRight - spec.horizontalPadding >= density * ToolbarMetrics.ClipboardPasteWidthDp,
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

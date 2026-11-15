package app.funput.funput.keyboard.layout.toolbar

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.SuggestionBarSpec
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState

/** Places toolbar slots from [ToolbarArrangement], yielding optional keys when the row is tight. */
internal object ToolbarGeometry {
    private const val TooNarrow = "Keyboard is too narrow for its required toolbar actions"

    fun resolve(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
        showClipboard: Boolean = false,
        showPlacement: Boolean = true,
        microphone: KeyboardMicrophoneState = KeyboardMicrophoneState(),
        arrangement: ToolbarArrangement = ToolbarArrangement.Default,
    ): ResolvedSuggestionBar? {
        val bar = layout.suggestionBar ?: return null
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val cell = spec.suggestionBarHeight
        val ordered = arrangement.normalize(availableSlots(bar, showClipboard, showPlacement, microphone.visible))
        val pending = (ordered.leading + ordered.trailing).toMutableList()
        val placed = linkedMapOf<ToolbarSlot, KeyBounds>()
        var left = spec.horizontalPadding
        var right = width - spec.horizontalPadding
        // Last trailing entry is flush right; list order stays left-to-right on screen.
        ordered.trailing.asReversed().forEachIndexed { index, slot ->
            placeTrailing(slot.yieldsWhenNarrow, index == 0, cell, top, bottom, spec, left, right, reserve(pending, slot, cell, spec.horizontalGap))
                ?.let { placed[slot] = it; right = it.left; pending.remove(slot) }
        }
        // Host-owned system IME switcher sits left of the trailing group when present.
        val systemBounds = bar.systemInputMethodKey?.let {
            placeTrailing(true, false, cell, top, bottom, spec, left, right, reserveRequired(pending, cell, spec.horizontalGap))
                ?.also { right = it.left }
        }
        ordered.leading.forEach { slot ->
            placeLeading(slot, cell, top, bottom, spec, left, right, reserve(pending, slot, cell, spec.horizontalGap))
                ?.let { placed[slot] = it; left = it.right + spec.horizontalGap; pending.remove(slot) }
        }
        require(ToolbarSlot.EMOJI in placed) { TooNarrow }
        val suggestionsLeft = left.coerceAtMost(right)
        val suggestionsRight = (right - if (bar.suggestionsEnabled) spec.horizontalGap else 0f).coerceAtLeast(suggestionsLeft)
        return ResolvedSuggestionBar(
            bounds = KeyBounds(spec.horizontalPadding, top, width - spec.horizontalPadding, bottom),
            suggestionsBounds = KeyBounds(suggestionsLeft, top, suggestionsRight, bottom),
            systemInputMethodKey = systemBounds?.let { ResolvedKey(requireNotNull(bar.systemInputMethodKey), it) },
            clipboardKey = placed[ToolbarSlot.CLIPBOARD]?.let { ResolvedKey(requireNotNull(bar.clipboardKey), it) },
            placementKey = placed[ToolbarSlot.PLACEMENT]?.let { ResolvedKey(bar.placementKey, it) },
            emojiKey = ResolvedKey(bar.emojiKey, requireNotNull(placed[ToolbarSlot.EMOJI])),
            microphoneKey = placed[ToolbarSlot.MICROPHONE]?.let {
                ResolvedKey(requireNotNull(bar.microphoneKey).copy(accessibilityLabel = microphone.accessibilityLabel), it, active = microphone.active)
            },
            suggestionsEnabled = bar.suggestionsEnabled,
        )
    }

    private fun reserve(pending: List<ToolbarSlot>, slot: ToolbarSlot, cell: Float, gap: Float) =
        if (slot.yieldsWhenNarrow) reserveRequired(pending.filter { it != slot }, cell, gap) else 0f

    private fun reserveRequired(pending: List<ToolbarSlot>, cell: Float, gap: Float): Float {
        val n = pending.count { !it.yieldsWhenNarrow }
        return if (n == 0) 0f else n * cell + (n - 1).coerceAtLeast(0) * gap
    }

    private fun availableSlots(
        bar: SuggestionBarSpec, showClipboard: Boolean, showPlacement: Boolean, microphoneVisible: Boolean,
    ) = buildSet {
        add(ToolbarSlot.EMOJI)
        if (microphoneVisible && bar.microphoneKey != null) add(ToolbarSlot.MICROPHONE)
        if (showPlacement) add(ToolbarSlot.PLACEMENT)
        if (showClipboard && bar.clipboardKey != null) add(ToolbarSlot.CLIPBOARD)
    }

    private fun placeLeading(
        slot: ToolbarSlot, cell: Float, top: Float, bottom: Float, spec: KeyboardGeometrySpec,
        leftAnchor: Float, rightAnchor: Float, reserve: Float,
    ): KeyBounds? {
        val bounds = KeyBounds(leftAnchor, top, leftAnchor + cell, bottom)
        val limit = rightAnchor - reserve
        if (bounds.right + spec.horizontalGap > limit && slot.yieldsWhenNarrow) return null
        require(bounds.right <= limit + 0.01f || !slot.yieldsWhenNarrow) { TooNarrow }
        return bounds.takeUnless { it.right > limit + 0.01f }
    }

    private fun placeTrailing(
        yields: Boolean, flush: Boolean, cell: Float, top: Float, bottom: Float, spec: KeyboardGeometrySpec,
        leftAnchor: Float, rightAnchor: Float, reserve: Float,
    ): KeyBounds? {
        val keyRight = rightAnchor - if (flush) 0f else spec.horizontalGap
        val bounds = KeyBounds(keyRight - cell, top, keyRight, bottom)
        val limit = leftAnchor + reserve
        if (bounds.left < limit + spec.horizontalGap && yields) return null
        require(bounds.left >= limit - 0.01f || !yields) { TooNarrow }
        return bounds.takeUnless { it.left < limit - 0.01f }
    }
}

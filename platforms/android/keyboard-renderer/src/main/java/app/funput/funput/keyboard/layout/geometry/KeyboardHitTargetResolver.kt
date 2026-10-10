package app.funput.funput.keyboard.layout.geometry

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar

/**
 * Expands visual key bounds to gap-free touch targets.
 *
 * Adjacent targets meet halfway through each visual gap. Outer keys extend to the surface edge,
 * matching the forgiving hit behavior users expect from a software keyboard.
 */
internal object KeyboardHitTargetResolver {
    fun resolve(keyboard: ResolvedKeyboard): ResolvedKeyboard {
        val suggestionBar = keyboard.suggestionBar?.let { bar -> resolveSuggestionBar(keyboard, bar) }
        val rows = keyboard.rows.mapIndexed { rowIndex, row ->
            val hitTop = rowHitTop(keyboard, rowIndex)
            val hitBottom = rowHitBottom(keyboard, rowIndex)
            row.mapIndexed { keyIndex, key ->
                key.copy(
                    hitBounds = KeyBounds(
                        left = keyHitLeft(row, keyIndex),
                        top = hitTop,
                        right = keyHitRight(row, keyIndex, keyboard.width),
                        bottom = hitBottom,
                    ),
                )
            }
        }
        return keyboard.copy(suggestionBar = suggestionBar, rows = rows)
    }

    private fun resolveSuggestionBar(
        keyboard: ResolvedKeyboard,
        bar: ResolvedSuggestionBar,
    ): ResolvedSuggestionBar {
        // Left to right: keyboard mode, voice input, suggestions, then the right-hand utilities.
        val leading = listOfNotNull(bar.placementKey, bar.microphoneKey)
        val trailing = listOfNotNull(bar.systemInputMethodKey, bar.clipboardKey, bar.emojiKey)
        val hitBottom = midpoint(bar.bounds.bottom, keyboard.rows.first().first().bounds.top)
        // Ordered spans of the toolbar; the suggestions area is a span too when enabled.
        val spans = leading.map { it.bounds } +
            (if (bar.suggestionsEnabled) listOf(bar.suggestionsBounds) else emptyList()) +
            trailing.map { it.bounds }
        fun edges(bounds: KeyBounds): Pair<Float, Float> {
            val index = spans.indexOf(bounds)
            val left = spans.getOrNull(index - 1)?.let { midpoint(it.right, bounds.left) } ?: 0f
            val right = spans.getOrNull(index + 1)?.let { midpoint(bounds.right, it.left) } ?: keyboard.width
            return left to right
        }
        val resolvedControls = (leading + trailing).map { key ->
            val (left, right) = edges(key.bounds)
            resolveToolbarKey(key, left, right, hitBottom)
        }
        fun lookup(key: ResolvedKey?): ResolvedKey? =
            key?.let { target -> resolvedControls.first { it.spec.id == target.spec.id } }
        val suggestionEdges = if (bar.suggestionsEnabled) edges(bar.suggestionsBounds)
            else bar.suggestionsBounds.left to bar.suggestionsBounds.right
        return bar.copy(
            suggestionsHitBounds = KeyBounds(
                left = suggestionEdges.first,
                top = 0f,
                right = suggestionEdges.second,
                bottom = hitBottom,
            ),
            systemInputMethodKey = lookup(bar.systemInputMethodKey),
            clipboardKey = lookup(bar.clipboardKey),
            placementKey = lookup(bar.placementKey),
            microphoneKey = lookup(bar.microphoneKey),
            emojiKey = requireNotNull(lookup(bar.emojiKey)),
        )
    }

    private fun resolveToolbarKey(
        key: ResolvedKey,
        left: Float,
        right: Float,
        bottom: Float,
    ): ResolvedKey = key.copy(
        hitBounds = KeyBounds(left = left, top = 0f, right = right, bottom = bottom),
    )

    private fun rowHitTop(keyboard: ResolvedKeyboard, rowIndex: Int): Float {
        val row = keyboard.rows[rowIndex]
        return if (rowIndex == 0) {
            keyboard.suggestionBar?.let { midpoint(it.bounds.bottom, row.first().bounds.top) } ?: 0f
        } else {
            midpoint(keyboard.rows[rowIndex - 1].first().bounds.bottom, row.first().bounds.top)
        }
    }

    private fun rowHitBottom(keyboard: ResolvedKeyboard, rowIndex: Int): Float {
        val row = keyboard.rows[rowIndex]
        return if (rowIndex == keyboard.rows.lastIndex) {
            keyboard.height
        } else {
            midpoint(row.first().bounds.bottom, keyboard.rows[rowIndex + 1].first().bounds.top)
        }
    }

    private fun keyHitLeft(row: List<ResolvedKey>, keyIndex: Int): Float = if (keyIndex == 0) {
        0f
    } else {
        midpoint(row[keyIndex - 1].bounds.right, row[keyIndex].bounds.left)
    }

    private fun keyHitRight(
        row: List<ResolvedKey>,
        keyIndex: Int,
        keyboardWidth: Float,
    ): Float = if (keyIndex == row.lastIndex) {
        keyboardWidth
    } else {
        midpoint(row[keyIndex].bounds.right, row[keyIndex + 1].bounds.left)
    }

    private fun midpoint(first: Float, second: Float): Float = (first + second) / 2f
}

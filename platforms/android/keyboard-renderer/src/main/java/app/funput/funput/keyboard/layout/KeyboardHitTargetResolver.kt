package app.funput.funput.keyboard.layout

/**
 * Expands visual key bounds to gap-free touch targets.
 *
 * Adjacent targets meet halfway through each visual gap. Outer keys extend to the surface edge,
 * matching the forgiving hit behavior users expect from a software keyboard.
 */
internal object KeyboardHitTargetResolver {
    fun resolve(keyboard: ResolvedKeyboard): ResolvedKeyboard {
        val suggestionBar = keyboard.suggestionBar?.let { bar ->
            bar.copy(
                settingsKey = resolveToolbarKey(
                    keyboard = keyboard,
                    key = bar.settingsKey,
                    left = if (bar.suggestionsEnabled) {
                        midpoint(bar.suggestionsBounds.right, bar.settingsKey.bounds.left)
                    } else {
                        midpoint(bar.bounds.left, bar.settingsKey.bounds.left)
                    },
                    right = midpoint(bar.settingsKey.bounds.right, bar.emojiKey.bounds.left),
                ),
                emojiKey = resolveToolbarKey(
                    keyboard = keyboard,
                    key = bar.emojiKey,
                    left = midpoint(bar.settingsKey.bounds.right, bar.emojiKey.bounds.left),
                    right = keyboard.width,
                ),
            )
        }
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
        return keyboard.copy(
            suggestionBar = suggestionBar,
            rows = rows,
        )
    }

    private fun resolveToolbarKey(
        keyboard: ResolvedKeyboard,
        key: ResolvedKey,
        left: Float,
        right: Float,
    ): ResolvedKey {
        return key.copy(
            hitBounds = KeyBounds(
                left = left,
                top = 0f,
                right = right,
                bottom = midpoint(key.bounds.bottom, keyboard.rows.first().first().bounds.top),
            ),
        )
    }

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

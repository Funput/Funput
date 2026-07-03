package app.funput.funput.keyboard.layout

/**
 * Expands visual key bounds to gap-free touch targets.
 *
 * Adjacent targets meet halfway through each visual gap. Outer keys extend to the surface edge,
 * matching the forgiving hit behavior users expect from a software keyboard.
 */
internal object KeyboardHitTargetResolver {
    fun resolve(keyboard: ResolvedKeyboard): ResolvedKeyboard {
        val emojiKey = resolveEmojiKey(keyboard)
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
            suggestionBar = keyboard.suggestionBar.copy(emojiKey = emojiKey),
            rows = rows,
        )
    }

    private fun resolveEmojiKey(keyboard: ResolvedKeyboard): ResolvedKey {
        val bar = keyboard.suggestionBar
        val key = bar.emojiKey
        return key.copy(
            hitBounds = KeyBounds(
                left = midpoint(bar.suggestionsBounds.right, key.bounds.left),
                top = 0f,
                right = keyboard.width,
                bottom = midpoint(key.bounds.bottom, keyboard.rows.first().first().bounds.top),
            ),
        )
    }

    private fun rowHitTop(keyboard: ResolvedKeyboard, rowIndex: Int): Float {
        val row = keyboard.rows[rowIndex]
        return if (rowIndex == 0) {
            midpoint(keyboard.suggestionBar.bounds.bottom, row.first().bounds.top)
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

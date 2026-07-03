package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardGeometryTest {
    private val spec = KeyboardGeometrySpec(
        horizontalPadding = 21f,
        verticalPadding = 24f,
        horizontalGap = 15f,
        verticalGap = 18f,
        suggestionBarHeight = 126f,
        suggestionBarGap = 18f,
    )

    @Test
    fun everyKeyStaysInsideKeyboardBounds() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            val keyboard = resolve(inputMethod)

            keyboard.keys.forEach { key ->
                assertTrue(key.bounds.left >= 0f)
                assertTrue(key.bounds.top >= 0f)
                assertTrue(key.bounds.right <= keyboard.width)
                assertTrue(key.bounds.bottom <= keyboard.height)
                assertTrue(key.bounds.width > 0f)
                assertTrue(key.bounds.height > 0f)
            }
        }
    }

    @Test
    fun keysNeverOverlapWithinARow() {
        KeyboardInputMethod.entries.forEach { inputMethod ->
            resolve(inputMethod).rows.forEach { row ->
                row.zipWithNext().forEach { (left, right) ->
                    assertTrue(left.bounds.right < right.bounds.left)
                }
            }
        }
    }

    @Test
    fun homeRowIsCenteredWithHalfUnitInsets() {
        val keyboard = resolve(KeyboardInputMethod.TELEX)
        val topRow = keyboard.rows[0]
        val homeRow = keyboard.rows[1]

        assertTrue(homeRow.first().bounds.left > topRow.first().bounds.left)
        assertTrue(homeRow.last().bounds.right < topRow.last().bounds.right)
    }

    @Test
    fun hitTestingReturnsResolvedKey() {
        val keyboard = resolve(KeyboardInputMethod.TELEX)
        val space = keyboard.keys.first { key -> key.spec.id == "space" }
        val firstCharacter = keyboard.rows.first().first()

        assertEquals(space, keyboard.keyAt(space.bounds.centerX, space.bounds.centerY))
        assertNotNull(keyboard.keyAt(firstCharacter.bounds.centerX, firstCharacter.bounds.centerY))
    }

    @Test
    fun suggestionBarPlacesEmojiAtTheRightEdge() {
        val keyboard = resolve(KeyboardInputMethod.TELEX)
        val suggestionBar = keyboard.suggestionBar

        assertEquals("emoji", suggestionBar.emojiKey.spec.id)
        assertTrue(suggestionBar.suggestionsBounds.right < suggestionBar.emojiKey.bounds.left)
        assertEquals(
            suggestionBar.emojiKey,
            keyboard.keyAt(suggestionBar.emojiKey.bounds.centerX, suggestionBar.emojiKey.bounds.centerY),
        )
    }

    private fun resolve(inputMethod: KeyboardInputMethod): ResolvedKeyboard = KeyboardGeometry.resolve(
        layout = KeyboardLayouts.forInputMethod(inputMethod),
        width = 1080f,
        height = if (inputMethod == KeyboardInputMethod.TELEX) 726f else 900f,
        spec = spec,
    )
}

package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardGeometryTest {
    private val spec = KeyboardGeometrySpec(
        horizontalPadding = 21f,
        verticalPadding = 24f,
        horizontalGap = 0f,
        verticalGap = 0f,
        horizontalGapRatio = 0.11f,
        verticalGapRatio = 0.16f,
        keyAspectRatio = 0.75f,
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
                assertTrue(key.hitBounds.left >= 0f)
                assertTrue(key.hitBounds.top >= 0f)
                assertTrue(key.hitBounds.right <= keyboard.width)
                assertTrue(key.hitBounds.bottom <= keyboard.height)
                assertTrue(key.hitBounds.width > 0f)
                assertTrue(key.hitBounds.height > 0f)
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
        val topRow = keyboard.rows[1]
        val homeRow = keyboard.rows[2]

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
    fun suggestionBarPlacesSettingsAndEmojiAtTheRightEdge() {
        val layout = qwertyLayout(
            id = "test-suggestions",
            inputMethod = KeyboardInputMethod.TELEX,
            actionKeys = KeyboardLayouts.telex.rows.last().keys,
            showSuggestionBar = true,
        ).copy(
            suggestionBar = requireNotNull(KeyboardLayouts.telex.suggestionBar).copy(
                suggestionsEnabled = true,
            ),
        )
        val keyboard = KeyboardGeometry.resolve(
            layout = layout,
            width = 1080f,
            height = 726f,
            spec = spec,
        )
        val suggestionBar = requireNotNull(keyboard.suggestionBar)
        val settings = requireNotNull(suggestionBar.settingsKey)

        assertEquals("settings", settings.spec.id)
        assertEquals("emoji", suggestionBar.emojiKey.spec.id)
        assertTrue(suggestionBar.logoBounds.width > 0f)
        assertTrue(suggestionBar.suggestionsBounds.left > suggestionBar.logoBounds.right)
        assertTrue(suggestionBar.suggestionsBounds.right < settings.bounds.left)
        assertTrue(settings.bounds.right < suggestionBar.emojiKey.bounds.left)
        assertEquals(
            settings,
            keyboard.keyAt(settings.bounds.centerX, settings.bounds.centerY),
        )
        assertEquals(
            suggestionBar.emojiKey,
            keyboard.keyAt(suggestionBar.emojiKey.bounds.centerX, suggestionBar.emojiKey.bounds.centerY),
        )
    }

    private fun resolve(inputMethod: KeyboardInputMethod): ResolvedKeyboard = KeyboardGeometry.resolve(
        layout = KeyboardLayouts.forInputMethod(inputMethod),
        width = 1080f,
        height = KeyboardDimensions.recommendedHeightDp(inputMethod) * (1080f / KeyboardDimensions.DefaultWidthDp),
        spec = spec,
    )

}

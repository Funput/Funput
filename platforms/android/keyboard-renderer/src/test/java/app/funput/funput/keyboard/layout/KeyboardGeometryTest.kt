package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyboardInputMethod
import kotlin.math.abs
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
        val suggestionBar = requireNotNull(keyboard.suggestionBar)

        assertEquals("emoji", suggestionBar.emojiKey.spec.id)
        assertTrue(suggestionBar.suggestionsBounds.right < suggestionBar.emojiKey.bounds.left)
        assertEquals(
            suggestionBar.emojiKey,
            keyboard.keyAt(suggestionBar.emojiKey.bounds.centerX, suggestionBar.emojiKey.bounds.centerY),
        )
    }

    @Test
    fun normalProfileKeepsCharacterKeyAspectRatioNearTarget() {
        val keyboard = resolveWithProfile(KeyboardInputMethod.TELEX, KeyboardSizingProfile.Normal)
        val characterKey = keyboard.rows.first().first()
        val aspectRatio = characterKey.bounds.width / characterKey.bounds.height

        assertTrue(aspectRatio in 0.72f..0.78f)
    }

    @Test
    fun normalProfileUsesRatioBasedHorizontalGap() {
        val profile = KeyboardSizingProfile.Normal
        val spec = KeyboardGeometrySpec.fromProfile(1f, profile)
        val width = KeyboardDimensions.DefaultWidthDp
        val height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.TELEX, profile = profile)
        val keyboard = KeyboardGeometry.resolve(
            layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX),
            width = width,
            height = height,
            spec = spec,
        )
        val topRow = keyboard.rows.first()
        val gap = topRow[1].bounds.left - topRow[0].bounds.right
        val canonicalUnit = topRow[0].bounds.width
        val gapRatio = gap / canonicalUnit

        assertTrue(abs(gapRatio - profile.horizontalGapRatio) <= 0.01f)
    }

    @Test
    fun sizingPresetsScaleKeyboardHeight() {
        val telex = KeyboardInputMethod.TELEX
        val normal = KeyboardDimensions.recommendedHeightDp(telex, profile = KeyboardSizingProfile.Normal)
        val compact = KeyboardDimensions.recommendedHeightDp(telex, profile = KeyboardSizingProfile.Compact)
        val large = KeyboardDimensions.recommendedHeightDp(telex, profile = KeyboardSizingProfile.Large)

        assertTrue(compact < normal)
        assertTrue(large > normal)
        assertEquals(normal * 0.92f, compact, 0.01f)
        assertEquals(normal * 1.08f, large, 0.01f)
    }

    private fun resolve(inputMethod: KeyboardInputMethod): ResolvedKeyboard = KeyboardGeometry.resolve(
        layout = KeyboardLayouts.forInputMethod(inputMethod),
        width = 1080f,
        height = if (inputMethod == KeyboardInputMethod.TELEX) 726f else 900f,
        spec = spec,
    )

    private fun resolveWithProfile(
        inputMethod: KeyboardInputMethod,
        profile: KeyboardSizingProfile,
    ): ResolvedKeyboard {
        val density = 1f
        return KeyboardGeometry.resolve(
            layout = KeyboardLayouts.forInputMethod(inputMethod),
            width = KeyboardDimensions.DefaultWidthDp * density,
            height = KeyboardDimensions.recommendedHeightDp(inputMethod, profile = profile) * density,
            spec = KeyboardGeometrySpec.fromProfile(density, profile),
        )
    }
}

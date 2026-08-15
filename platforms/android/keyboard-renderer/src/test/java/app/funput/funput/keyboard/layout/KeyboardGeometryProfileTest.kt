package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardGeometryProfileTest {
    @Test
    fun normalProfileKeepsCharacterKeyAspectRatioNearTarget() {
        val keyboard = resolveWithProfile(KeyboardSizingProfile.Normal)
        val key = keyboard.rows.first().first()

        assertTrue(key.bounds.width / key.bounds.height in 0.72f..0.78f)
    }

    @Test
    fun normalProfileUsesRatioBasedHorizontalGap() {
        val profile = KeyboardSizingProfile.Normal
        val keyboard = resolveWithProfile(profile)
        val topRow = keyboard.rows.first()
        val gap = topRow[1].bounds.left - topRow[0].bounds.right
        val gapRatio = gap / topRow[0].bounds.width

        assertTrue(abs(gapRatio - profile.horizontalGapRatio) <= 0.01f)
    }

    @Test
    fun sizingScaleChangesKeyboardHeight() {
        val method = KeyboardInputMethod.TELEX
        val normal = KeyboardDimensions.recommendedHeightDp(method, profile = KeyboardSizingProfile.Normal)
        val compact = KeyboardDimensions.recommendedHeightDp(
            method,
            profile = KeyboardSizingProfile.scaled(0.92f),
        )
        val large = KeyboardDimensions.recommendedHeightDp(
            method,
            profile = KeyboardSizingProfile.scaled(1.08f),
        )

        assertTrue(compact < normal)
        assertTrue(large > normal)
        assertEquals(normal * 0.92f, compact, 0.01f)
        assertEquals(normal * 1.08f, large, 0.01f)
    }

    @Test
    fun vniSymbolLayerKeepsBottomRowAlignedWithLetters() {
        val letters = resolveVni(KeyboardLayoutMode.LETTERS)
        val symbols = resolveVni(KeyboardLayoutMode.SYMBOLS_PRIMARY)
        val lettersSpace = letters.keys.first { it.spec.id == "space" }
        val symbolsSpace = symbols.keys.first { it.spec.id == "space-primary" }

        assertEquals(lettersSpace.bounds.top, symbolsSpace.bounds.top, 0.5f)
        assertEquals(lettersSpace.bounds.bottom, symbolsSpace.bounds.bottom, 0.5f)
        assertEquals(letters.rows[1].first().bounds.top, symbols.rows[1].first().bounds.top, 0.5f)
    }

    @Test
    fun narrowFoldCoverLayoutsFillTheSameRecommendedHeight() {
        val width = 344f
        val height = KeyboardDimensions.recommendedHeightDp(
            KeyboardInputMethod.VNI,
            widthDp = width,
        )
        val spec = KeyboardGeometrySpec.fromDensity(1f)

        listOf(KeyboardLayoutMode.LETTERS, KeyboardLayoutMode.SYMBOLS_PRIMARY).forEach { mode ->
            val keyboard = KeyboardGeometry.resolve(
                KeyboardLayoutResolver.resolve(KeyboardInputMethod.VNI, mode),
                width,
                height,
                spec,
            )
            assertEquals(height - spec.verticalPadding, keyboard.rows.last().first().bounds.bottom, 0.5f)
        }
    }

    @Test
    fun wideFoldScreenDoesNotGrowPastThePhoneKeyboardHeight() {
        val phoneHeight = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.TELEX)
        val innerScreenHeight = KeyboardDimensions.recommendedHeightDp(
            KeyboardInputMethod.TELEX,
            widthDp = 690f,
        )

        assertEquals(phoneHeight, innerScreenHeight, 0.01f)
    }

    private fun resolveWithProfile(profile: KeyboardSizingProfile): ResolvedKeyboard = KeyboardGeometry.resolve(
        layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX),
        width = KeyboardDimensions.DefaultWidthDp,
        height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.TELEX, profile = profile),
        spec = KeyboardGeometrySpec.fromProfile(1f, profile),
    )

    private fun resolveVni(mode: KeyboardLayoutMode): ResolvedKeyboard = KeyboardGeometry.resolve(
        layout = KeyboardLayoutResolver.resolve(KeyboardInputMethod.VNI, mode),
        width = KeyboardDimensions.DefaultWidthDp,
        height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.VNI),
        spec = KeyboardGeometrySpec.fromProfile(1f, KeyboardSizingProfile.Normal),
    )
}

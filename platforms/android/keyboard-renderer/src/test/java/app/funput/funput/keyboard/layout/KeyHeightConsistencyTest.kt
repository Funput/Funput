package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A single key size for the whole app: at any one sizing profile every editor mode, layout page and
 * number-row preference must resolve to the same key height. A panel measured for the wrong row
 * count squeezes its rows instead, which reads as the size setting being ignored.
 */
class KeyHeightConsistencyTest {
    @Test
    fun everyModeResolvesTheSameKeyHeight() {
        for (profile in listOf(
            KeyboardSizingProfile.Normal,
            KeyboardSizingProfile.Default,
            KeyboardSizingProfile.scaled(KeyboardSizingProfile.MinScale),
            KeyboardSizingProfile.scaled(KeyboardSizingProfile.MaxScale),
        )) {
            val spec = KeyboardGeometrySpec.fromProfile(1f, profile)
            val expected = canonicalKeyHeight(spec)
            forEachCombination { inputMethod, editorMode, layoutMode, showsNumberRow ->
                if (!reachable(inputMethod, editorMode, layoutMode, showsNumberRow)) return@forEachCombination
                val keyHeight = resolveKeyHeight(
                    inputMethod, editorMode, layoutMode, showsNumberRow, profile, spec,
                )

                assertEquals(
                    "$inputMethod/$editorMode/$layoutMode numberRow=$showsNumberRow " +
                        "at scale ${profile.heightScale}",
                    expected,
                    keyHeight,
                    0.5f,
                )
            }
        }
    }

    /** The keypad editors ship no "?123" key, so their symbol pages are unreachable in the IME. */
    private fun reachable(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        layoutMode: KeyboardLayoutMode,
        showsNumberRow: Boolean,
    ): Boolean = layoutMode == KeyboardLayoutMode.LETTERS ||
        KeyboardLayoutResolver.resolve(
            inputMethod = inputMethod,
            mode = KeyboardLayoutMode.LETTERS,
            editorMode = editorMode,
            showsNumberRow = showsNumberRow,
        ).rows.any { row -> row.keys.any { it.role == KeyRole.SYMBOLS } }

    private fun canonicalKeyHeight(spec: KeyboardGeometrySpec): Float {
        val contentWidth = KeyboardDimensions.DefaultWidthDp - spec.horizontalPadding * 2f
        val canonicalUnit = contentWidth / (10 + 9 * spec.horizontalGapRatio)
        return canonicalUnit / spec.keyAspectRatio * spec.heightScale
    }

    private fun resolveKeyHeight(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        layoutMode: KeyboardLayoutMode,
        showsNumberRow: Boolean,
        profile: KeyboardSizingProfile,
        spec: KeyboardGeometrySpec,
    ): Float {
        val layout = KeyboardLayoutResolver.resolve(
            inputMethod = inputMethod,
            mode = layoutMode,
            editorMode = editorMode,
            showsNumberRow = showsNumberRow,
        )
        val keyboard = KeyboardGeometry.resolve(
            layout = layout,
            width = KeyboardDimensions.DefaultWidthDp,
            height = KeyboardDimensions.recommendedHeightDp(
                inputMethod = inputMethod,
                editorMode = editorMode,
                profile = profile,
                showsNumberRow = showsNumberRow,
            ),
            spec = spec,
        )
        val key = keyboard.rows.first().first().bounds
        return key.bottom - key.top
    }

    private fun forEachCombination(
        block: (KeyboardInputMethod, KeyboardEditorMode, KeyboardLayoutMode, Boolean) -> Unit,
    ) {
        for (inputMethod in KeyboardInputMethod.entries) {
            for (editorMode in KeyboardEditorMode.entries) {
                for (layoutMode in KeyboardLayoutMode.entries) {
                    for (showsNumberRow in listOf(true, false)) {
                        block(inputMethod, editorMode, layoutMode, showsNumberRow)
                    }
                }
            }
        }
    }
}

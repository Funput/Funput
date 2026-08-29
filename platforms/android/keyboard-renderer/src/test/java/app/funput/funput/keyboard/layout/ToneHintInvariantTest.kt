package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tone hints promise an effect, so they must appear exactly where that effect exists: on every
 * editor that composes Vietnamese, and on no editor that does not. A search field that composes but
 * hides its hints is as wrong as an email field that advertises tones it will never apply.
 */
class ToneHintInvariantTest {
    @Test
    fun toneHintsAppearExactlyWhereCompositionDoes() {
        forEachLettersLayout { inputMethod, editorMode, showsNumberRow, layout ->
            val expected = editorMode.supportsVietnameseComposition
            val label = "$inputMethod/$editorMode numberRow=$showsNumberRow"

            if (inputMethod.isTelexFamily) {
                assertEquals("$label telex hints", expected, layout.hasTelexHints())
            } else {
                assertEquals("$label VNI hints", expected, layout.hasVniModifiers())
            }
        }
    }

    @Test
    fun theBrowserAddressBarKeepsItsHints() {
        // A URI field resolves to SEARCH, which composes Vietnamese; regression guard for hints
        // that were dropped because the web layouts are built apart from the text layout.
        for (inputMethod in KeyboardInputMethod.entries) {
            val layout = lettersLayout(inputMethod, KeyboardEditorMode.SEARCH, showsNumberRow = true)

            assertEquals(
                "$inputMethod search hints",
                true,
                if (inputMethod.isTelexFamily) layout.hasTelexHints() else layout.hasVniModifiers(),
            )
        }
    }

    /**
     * A compact page prints the long-press digit in the same slot, and a digit promises nothing
     * about tones — only the rest of the hint counts as one.
     */
    private fun KeyboardLayout.hasTelexHints(): Boolean = rows.flatMap { it.keys }.any { key ->
        key.role == KeyRole.CHARACTER &&
            key.secondaryLabel.orEmpty().any { !it.isDigit() && !it.isWhitespace() }
    }

    private fun KeyboardLayout.hasVniModifiers(): Boolean = rows.flatMap { it.keys }.any { key ->
        key.role == KeyRole.VNI_MODIFIER && key.secondaryLabel != null
    }

    private fun lettersLayout(
        inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Boolean,
    ): KeyboardLayout = KeyboardLayoutResolver.resolve(
        inputMethod = inputMethod,
        mode = KeyboardLayoutMode.LETTERS,
        editorMode = editorMode,
        showsNumberRow = showsNumberRow,
    )

    private fun forEachLettersLayout(
        block: (KeyboardInputMethod, KeyboardEditorMode, Boolean, KeyboardLayout) -> Unit,
    ) {
        for (inputMethod in KeyboardInputMethod.entries) {
            for (editorMode in KeyboardEditorMode.entries) {
                for (showsNumberRow in listOf(true, false)) {
                    block(
                        inputMethod,
                        editorMode,
                        showsNumberRow,
                        lettersLayout(inputMethod, editorMode, showsNumberRow),
                    )
                }
            }
        }
    }
}

package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelexToneHintsLayoutTest {
    private val expectedHints = mapOf(
        "s" to "´",
        "f" to "`",
        "r" to "̉",
        "x" to "˜",
        "j" to "̣",
        "z" to "×",
    )

    @Test
    fun telexLettersExposeToneHintsWithoutChangingSemantics() {
        assertToneHints(KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX))
    }

    @Test
    fun advancedTelexMatchesTelexToneHints() {
        assertToneHints(KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX_ADVANCED))
    }

    @Test
    fun compactTelexStillShowsToneHints() {
        assertToneHints(
            KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX, showsNumberRow = false),
        )
    }

    @Test
    fun vniLettersDoNotShowToneHints() {
        val letterKeys = letterKeys(KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI))

        assertTrue(letterKeys.none { it.secondaryLabel != null })
        assertTrue(
            KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI).rows.first()
                .keys.all { it.secondaryLabel != null },
        )
    }

    @Test
    fun specializedEditorsHideTelexHints() {
        listOf(
            KeyboardEditorMode.SEARCH,
            KeyboardEditorMode.EMAIL,
            KeyboardEditorMode.URL,
            KeyboardEditorMode.PASSWORD,
        ).forEach { editorMode ->
            val layout = KeyboardLayoutResolver.resolve(
                inputMethod = KeyboardInputMethod.TELEX,
                mode = KeyboardLayoutMode.LETTERS,
                editorMode = editorMode,
            )
            assertTrue(
                "$editorMode",
                letterKeys(layout).none { it.secondaryLabel != null },
            )
        }
    }

    private fun assertToneHints(layout: app.funput.funput.keyboard.model.KeyboardLayout) {
        val letterKeys = letterKeys(layout)
        val hinted = letterKeys
            .filter { it.secondaryLabel != null }
            .associate { it.label to it.secondaryLabel }

        assertEquals(expectedHints, hinted)
        letterKeys.filter { it.label in expectedHints }.forEach { key ->
            assertEquals(KeyRole.CHARACTER, key.role)
            assertEquals(1f, key.widthWeight)
            assertEquals("character-${key.label}", key.id)
            assertEquals(
                TelexKeyHints.accessibilityLabel(key.label.single()),
                key.accessibilityLabel,
            )
        }
        letterKeys.filter { it.label !in expectedHints }.forEach { key ->
            assertNull(key.secondaryLabel)
        }
    }

    private fun letterKeys(layout: app.funput.funput.keyboard.model.KeyboardLayout) =
        layout.rows.flatMap { it.keys }.filter { key ->
            key.role == KeyRole.CHARACTER && key.label.length == 1 && key.label[0].isLetter()
        }
}

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
    fun compactTelexKeepsToneHintsBesideItsDigitHints() {
        // With the number row hidden the top row also carries digits, so `r` reads "hỏi 4"
        // and its nine neighbours gain a hint they do not have on the full layout.
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX, showsNumberRow = false)
        val hinted = letterKeys(layout)
            .filter { it.secondaryLabel != null }
            .associate { it.label to it.secondaryLabel }

        expectedHints.forEach { (label, glyph) ->
            val hint = requireNotNull(hinted[label]) { label }
            assertTrue(label, hint.startsWith(glyph))
        }
        assertEquals("̉ 4", hinted["r"])
        assertEquals("´", hinted["s"])
        // Only the top row and the tone keys are hinted; the rest stay bare.
        assertTrue(
            letterKeys(layout)
                .filter { it.label !in expectedHints && it.label !in TopRowLabels }
                .all { it.secondaryLabel == null },
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
    fun searchEditorsKeepTelexHints() {
        // A browser address bar arrives as SEARCH and still composes Vietnamese, so the hints hold.
        assertToneHints(
            KeyboardLayoutResolver.resolve(
                inputMethod = KeyboardInputMethod.TELEX,
                mode = KeyboardLayoutMode.LETTERS,
                editorMode = KeyboardEditorMode.SEARCH,
            ),
        )
    }

    @Test
    fun nonComposingEditorsHideTelexHints() {
        listOf(
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

    private companion object {
        val TopRowLabels = "qwertyuiop".map(Char::toString)
    }

    private fun letterKeys(layout: app.funput.funput.keyboard.model.KeyboardLayout) =
        layout.rows.flatMap { it.keys }.filter { key ->
            key.role == KeyRole.CHARACTER && key.label.length == 1 && key.label[0].isLetter()
        }
}

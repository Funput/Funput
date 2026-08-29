package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Search, email and URL answer the number-row preference exactly as the letters page does. */
class WebEditorNumberRowLayoutTest {
    @Test
    fun `telex web pages drop the number row when the preference is off`() {
        forEachWebEditor { editorMode ->
            listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.TELEX_ADVANCED).forEach { method ->
                val layout = resolve(editorMode, method, showsNumberRow = false)
                val name = "${editorMode.name.lowercase()}-${method.name.lowercase()}"

                assertEquals("qwerty-$name-compact", layout.id)
                assertEquals("$name rows", 4, layout.rows.size)
                assertFalse("$name digit row", layout.rows.any { row -> row.keys.all(::isDigitKey) })
            }
        }
    }

    @Test
    fun `hidden digits come back as long-press alternates, beside any tone hint`() {
        forEachWebEditor { editorMode ->
            val compact = topRow(resolve(editorMode, showsNumberRow = false))
            val standard = topRow(resolve(editorMode, showsNumberRow = true))

            assertEquals(Digits, compact.keys.map { it.alternates.first().text })
            compact.keys.forEachIndexed { index, key ->
                // Search composes, so its tone hint keeps its slot and the digit joins it on the
                // right; email and URL never had one, so the digit stands alone.
                val tone = standard.keys[index].secondaryLabel
                assertEquals(
                    "$editorMode hint $index",
                    tone?.let { "$it ${Digits[index]}" } ?: Digits[index],
                    key.secondaryLabel,
                )
            }
        }
    }

    @Test
    fun `the preference on leaves every web page exactly as it was`() {
        forEachWebEditor { editorMode ->
            KeyboardInputMethod.entries.forEach { method ->
                val layout = resolve(editorMode, method, showsNumberRow = true)
                val name = "${editorMode.name.lowercase()}-${method.name.lowercase()}"

                assertEquals("qwerty-$name", layout.id)
                assertEquals("$name rows", 5, layout.rows.size)
                assertTrue("$name digit row", layout.rows.first().keys.all(::isDigitKey))
            }
        }
    }

    @Test
    fun `vni keeps its digit row whatever the preference is`() {
        forEachWebEditor { editorMode ->
            val layout = resolve(editorMode, KeyboardInputMethod.VNI, showsNumberRow = false)
            // Only a composing page spends that row on tone modifiers.
            val role = if (editorMode.supportsVietnameseComposition) {
                KeyRole.VNI_MODIFIER
            } else {
                KeyRole.CHARACTER
            }

            assertEquals("qwerty-${editorMode.name.lowercase()}-vni", layout.id)
            assertEquals(5, layout.rows.size)
            assertTrue("$editorMode row role", layout.rows.first().keys.all { it.role == role })
            assertTrue("$editorMode alternates", topRow(layout).keys[0].alternates.none { it.text == "1" })
        }
    }

    @Test
    fun `the symbols page still shows the digits, in the same panel height`() {
        forEachWebEditor { editorMode ->
            val letters = resolve(editorMode, showsNumberRow = false)
            val symbols = KeyboardLayoutResolver.resolve(
                inputMethod = KeyboardInputMethod.TELEX,
                mode = KeyboardLayoutMode.SYMBOLS_PRIMARY,
                editorMode = editorMode,
                showsNumberRow = false,
            )

            assertTrue("$editorMode compact symbols", symbols.id.contains("compact"))
            assertEquals("$editorMode rows", letters.rows.size, symbols.rows.size)
            assertTrue("$editorMode digits", symbols.rows.first().keys.all(::isDigitKey))
        }
    }

    @Test
    fun `the measured height follows the row the page dropped`() {
        forEachWebEditor { editorMode ->
            val compact = height(editorMode, showsNumberRow = false)

            assertTrue("$editorMode shorter", compact < height(editorMode, showsNumberRow = true))
            assertEquals(
                "$editorMode matches the letters page",
                height(KeyboardEditorMode.TEXT, showsNumberRow = false),
                compact,
                0.001f,
            )
        }
    }

    private fun forEachWebEditor(block: (KeyboardEditorMode) -> Unit) = listOf(
        KeyboardEditorMode.SEARCH,
        KeyboardEditorMode.EMAIL,
        KeyboardEditorMode.URL,
    ).forEach(block)

    private fun resolve(
        editorMode: KeyboardEditorMode,
        method: KeyboardInputMethod = KeyboardInputMethod.TELEX,
        showsNumberRow: Boolean,
    ): KeyboardLayout = KeyboardLayoutResolver.resolve(
        inputMethod = method,
        mode = KeyboardLayoutMode.LETTERS,
        editorMode = editorMode,
        showsNumberRow = showsNumberRow,
    )

    private fun height(editorMode: KeyboardEditorMode, showsNumberRow: Boolean): Float =
        KeyboardDimensions.recommendedHeightDp(
            KeyboardInputMethod.TELEX,
            editorMode,
            showsNumberRow = showsNumberRow,
        )

    private fun topRow(layout: KeyboardLayout) =
        layout.rows.first { row -> row.keys.map { it.label } == TopRow.map(Char::toString) }

    private companion object {
        const val TopRow = "qwertyuiop"
        val Digits = "1234567890".map(Char::toString)
    }
}

private fun isDigitKey(key: KeySpec): Boolean = key.label.singleOrNull()?.isDigit() == true

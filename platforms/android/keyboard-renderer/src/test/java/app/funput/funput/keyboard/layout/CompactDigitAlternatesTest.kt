package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardRow
import app.funput.funput.keyboard.popover.model.VietnameseKeyAlternates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactDigitAlternatesTest {
    @Test
    fun `top row prints its digit, beside a Telex hint where there is one`() {
        val compact = compactRow(KeyboardInputMethod.TELEX)
        val standard = topRow(KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX))

        compact.keys.forEachIndexed { index, key ->
            val tone = standard.keys[index].secondaryLabel
            assertEquals(
                tone?.let { "$it ${Digits[index]}" } ?: Digits[index],
                key.secondaryLabel,
            )
        }
        // `r` is the hỏi key: the tone hint leads and the digit follows it.
        val hint = requireNotNull(compact.keys[3].secondaryLabel)
        assertTrue(hint.startsWith(requireNotNull(standard.keys[3].secondaryLabel)))
        assertTrue(hint.endsWith("4"))
    }

    @Test
    fun `the digit leads every top-row palette`() {
        listOf(KeyboardInputMethod.TELEX, KeyboardInputMethod.TELEX_ADVANCED).forEach { method ->
            val row = compactRow(method)
            row.keys.forEachIndexed { index, key ->
                assertEquals(Digits[index], key.alternates.first().text)
                assertEquals(Digits[index], key.alternates.first().shiftedText)
                assertTrue(key.accessibilityLabel.endsWith(", số ${Digits[index]}"))
            }
            assertEquals(
                listOf("7") + VietnameseKeyAlternates.valuesFor('u').map { it.text },
                row.keys[6].alternates.map { it.text },
            )
            assertEquals(listOf("1"), row.keys[0].alternates.map { it.text })
        }
    }

    @Test
    fun `keys keep their identity so geometry and layout ids do not move`() {
        val layout = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX, showsNumberRow = false)
        val row = topRow(layout)

        assertEquals("qwerty-telex-compact", layout.id)
        assertEquals(TopRow.map { "character-$it" }, row.keys.map { it.id })
        assertTrue(row.keys.all { it.role == KeyRole.CHARACTER && it.widthWeight == 1f })
        assertEquals(TopRow.uppercase().map(Char::toString), row.keys.map { it.shiftedLabel })
    }

    @Test
    fun `digits stay off whenever a number row is on screen`() {
        KeyboardInputMethod.entries.forEach { method ->
            val row = topRow(KeyboardLayouts.forInputMethod(method))
            assertEquals(null, row.keys[0].secondaryLabel)
            assertTrue(row.keys[0].alternates.isEmpty())
            assertEquals("u", row.keys[6].alternates.first().text)
        }
        // VNI forces its own digit row, so the preference never reaches this page.
        val vni = topRow(
            KeyboardLayouts.forInputMethod(KeyboardInputMethod.VNI, showsNumberRow = false),
        )
        assertEquals(null, vni.keys[0].secondaryLabel)
        assertTrue(vni.keys[0].alternates.isEmpty())
    }

    @Test
    fun `only the compact letters page changes`() {
        val compact = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX, showsNumberRow = false)
        val standard = KeyboardLayouts.forInputMethod(KeyboardInputMethod.TELEX)
        listOf("a", "s", "z").forEach { label ->
            val key = compact.rows.flatMap { it.keys }.first { it.label == label }
            val reference = standard.rows.flatMap { it.keys }.first { it.label == label }
            assertEquals(reference.secondaryLabel, key.secondaryLabel)
            assertEquals(reference.accessibilityLabel, key.accessibilityLabel)
            assertEquals(reference.alternates, key.alternates)
        }
        // The QWERTY editors follow the preference too — WebEditorNumberRowLayoutTest covers
        // them. A page with no number row to trade keeps its top row exactly as it was.
        val password = topRow(
            KeyboardLayoutResolver.resolve(
                inputMethod = KeyboardInputMethod.TELEX,
                mode = KeyboardLayoutMode.LETTERS,
                editorMode = KeyboardEditorMode.PASSWORD,
                showsNumberRow = false,
            ),
        )
        assertEquals(null, password.keys[0].secondaryLabel)
        assertTrue(password.keys[0].alternates.isEmpty())
    }

    private fun compactRow(method: KeyboardInputMethod): KeyboardRow =
        topRow(KeyboardLayouts.forInputMethod(method, showsNumberRow = false))

    private fun topRow(layout: KeyboardLayout): KeyboardRow =
        layout.rows.first { row -> row.keys.map { it.label } == TopRow.map(Char::toString) }

    private companion object {
        const val TopRow = "qwertyuiop"
        val Digits = "1234567890".map(Char::toString)
    }
}

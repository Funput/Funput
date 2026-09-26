package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.layout.keys.periodKey
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.toKeyAction
import app.funput.funput.keyboard.popover.model.KeyAlternate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodKeyTest {
    @Test
    fun `period catalog excludes comma and a tap still types a period`() {
        val key = periodKey("period")

        assertEquals(
            listOf("@", "&", "%", "+", ";", "/", "(", ")", "\"", "'", "#", "-", ":", "!", "?"),
            key.alternates.map { (it as KeyAlternate.Text).text },
        )
        assertEquals(null, key.preferredAlternateText)
        assertEquals(8, key.alternatePaletteColumns)
        assertEquals(KeyAction.Input("period", "."), key.toKeyAction(ShiftState.OFF))
    }

    @Test
    fun `all action-row period keys share the catalog`() {
        val editors = listOf(
            KeyboardEditorMode.TEXT,
            KeyboardEditorMode.SEARCH,
            KeyboardEditorMode.EMAIL,
            KeyboardEditorMode.URL,
            KeyboardEditorMode.PASSWORD,
        )
        KeyboardInputMethod.entries.forEach { method ->
            listOf(true, false).forEach { numberRow ->
                editors.forEach { editor ->
                    assertPeriod(method, KeyboardLayoutMode.LETTERS, editor, numberRow)
                }
                KeyboardLayoutMode.entries.filter { it != KeyboardLayoutMode.LETTERS }.forEach { mode ->
                    assertPeriod(method, mode, KeyboardEditorMode.TEXT, numberRow)
                    assertPeriod(method, mode, KeyboardEditorMode.PASSWORD, numberRow)
                }
            }
        }
    }

    @Test
    fun `decimal keypad period has no alternate palette`() {
        listOf(KeyboardEditorMode.NUMBER_DECIMAL, KeyboardEditorMode.NUMBER_SIGNED_DECIMAL).forEach { editor ->
            val layout = KeyboardLayoutResolver.resolve(
                KeyboardInputMethod.TELEX, KeyboardLayoutMode.LETTERS, editor,
            )
            val period = layout.rows.flatMap { it.keys }.single { it.id == "period" }

            assertEquals(".", period.label)
            assertTrue(period.alternates.isEmpty())
        }
    }

    private fun assertPeriod(
        method: KeyboardInputMethod,
        mode: KeyboardLayoutMode,
        editor: KeyboardEditorMode,
        numberRow: Boolean,
    ) {
        val layout = KeyboardLayoutResolver.resolve(
            method, mode, editor, showsNumberRow = numberRow,
        )
        val period = layout.rows.last().keys.single { it.label == "." }
        val expected = periodKey(period.id)

        assertEquals("$method $mode $editor $numberRow", expected, period)
    }
}

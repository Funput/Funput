package app.funput.funput.ime.editing.caret

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The arithmetic behind a vertical step. Whether the caret then lands on the right visual line is
 * the editor's behaviour, covered end to end in `CaretPanInstrumentedTest`.
 *
 * Asserts on the plan rather than on `KeyEvent`s: android.jar's getters throw in a JVM unit test,
 * which is why the sending half is left to the instrumented side.
 */
class CaretLineKeysTest {
    @Test
    fun upBecomesOneArrowPerLine() {
        assertEquals(
            CaretLineKeys.Plan(KeyEvent.KEYCODE_DPAD_UP, presses = 2),
            CaretLineKeys.plan(-2),
        )
    }

    @Test
    fun downBecomesTheOppositeArrow() {
        assertEquals(
            CaretLineKeys.Plan(KeyEvent.KEYCODE_DPAD_DOWN, presses = 1),
            CaretLineKeys.plan(1),
        )
    }

    @Test
    fun noLinesPlansNothing() {
        assertNull(CaretLineKeys.plan(0))
    }

    @Test
    fun aWildStepIsCappedRatherThanFloodingTheEditor() {
        assertEquals(8, CaretLineKeys.plan(-400)?.presses)
    }
}

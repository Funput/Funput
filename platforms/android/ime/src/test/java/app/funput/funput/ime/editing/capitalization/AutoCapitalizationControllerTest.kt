package app.funput.funput.ime.editing.capitalization

import android.text.TextUtils
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCapitalizationControllerTest {
    private var textBefore: CharSequence? = ""
    private var atBoundary = true
    private var shiftState = ShiftState.OFF
    private val asked = mutableListOf<Pair<AutoCapitalizationMode, String>>()

    /** Stands in for the Rust rules so the test needs no native library. */
    private val boundary = CapitalizationBoundary { mode, before ->
        asked += mode to before.toString()
        when (mode) {
            AutoCapitalizationMode.NONE -> false
            AutoCapitalizationMode.ALL_CHARACTERS -> true
            else -> atBoundary
        }
    }

    private val controller = AutoCapitalizationController(
        boundary = boundary,
        textBeforeCursor = { textBefore },
        currentShiftState = { shiftState },
        updateShiftState = { value -> shiftState = value },
    )

    @Test
    fun `an editor that sets no cap flag still capitalizes sentences`() {
        controller.configure(EditorInfoPolicy.Default)

        controller.update()

        assertEquals(ShiftState.ON, shiftState)
        assertEquals(AutoCapitalizationMode.SENTENCES, asked.single().first)
    }

    @Test
    fun `leaving a sentence start lowers shift again`() {
        controller.configure(EditorInfoPolicy.Default)
        controller.update()

        atBoundary = false
        controller.update()

        assertEquals(ShiftState.OFF, shiftState)
    }

    @Test
    fun `the text before the caret is what gets asked about`() {
        textBefore = "Xin chào. "
        controller.configure(EditorInfoPolicy.Default)

        controller.update()

        assertEquals("Xin chào. ", asked.single().second)
    }

    @Test
    fun `a missing input connection reads as the start of the document`() {
        textBefore = null
        controller.configure(EditorInfoPolicy.Default)

        controller.update()

        assertEquals("", asked.single().second)
    }

    @Test
    fun `disabling the preference silences sentences`() {
        controller.configure(EditorInfoPolicy.Default)
        controller.setEnabled(false)

        controller.update()

        assertEquals(ShiftState.OFF, shiftState)
    }

    @Test
    fun `re-enabling the preference takes effect without a new editor`() {
        controller.configure(EditorInfoPolicy.Default)
        controller.setEnabled(false)
        controller.setEnabled(true)

        controller.update()

        assertEquals(ShiftState.ON, shiftState)
    }

    @Test
    fun `a field demanding all caps still shifts when the preference is off`() {
        controller.configure(
            EditorInfoPolicy.Default.copy(capitalizationModes = TextUtils.CAP_MODE_CHARACTERS),
        )
        controller.setEnabled(false)
        atBoundary = false

        controller.update()

        assertEquals(ShiftState.ON, shiftState)
    }

    @Test
    fun `no editor capitalizes nothing`() {
        controller.configure(policy = null)

        controller.update()

        assertEquals(ShiftState.OFF, shiftState)
        assertEquals(AutoCapitalizationMode.NONE, asked.single().first)
    }

    @Test
    fun `selection updates preserve manual caps lock`() {
        shiftState = ShiftState.CAPS_LOCK
        controller.configure(EditorInfoPolicy.Default)

        controller.update()

        assertEquals(ShiftState.CAPS_LOCK, shiftState)
    }

    @Test
    fun `a new editor resets caps lock to its requested state`() {
        shiftState = ShiftState.CAPS_LOCK
        controller.configure(policy = null)

        controller.update(preserveCapsLock = false)

        assertEquals(ShiftState.OFF, shiftState)
    }
}

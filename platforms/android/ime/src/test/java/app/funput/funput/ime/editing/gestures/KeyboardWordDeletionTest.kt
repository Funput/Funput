package app.funput.funput.ime.editing.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class KeyboardWordDeletionSpanTest(
    private val context: String,
    private val expected: Int,
) {
    @Test
    fun spanBehindTheCaretCoversTrailingSpacesAndTheWholeWord() {
        assertEquals(expected, KeyboardWordDeletion.spanBeforeCursor(context))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun cases(): List<Array<Any>> = listOf(
            arrayOf("xin chào", 4),
            arrayOf("xin chào ", 5),
            arrayOf("xin   ", 6),
            arrayOf("a!!!", 3),
        )
    }
}

class KeyboardWordDeletionTest {
    @Test
    fun emptyOrUnknownContextHasNoMeasurableSpan() {
        assertNull(KeyboardWordDeletion.spanBeforeCursor(""))
        assertNull(KeyboardWordDeletion.spanBeforeCursor(null))
    }

    @Test
    fun aNewlineIsNotTrailingSpaceToSwallow() {
        assertNull(KeyboardWordDeletion.spanBeforeCursor("xin\n"))
    }
}

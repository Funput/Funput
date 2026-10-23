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
            arrayOf("https://github.com/Funput/Funput/issues/536", 3),
            arrayOf("https://github.com/Funput/Funput/issues/", 1),
            arrayOf("https://github.com/Funput/Funput/issues", 6),
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

    @Test
    fun unbrokenLongTokenDeletesInBoundedChunks() {
        val longToken = "a".repeat(60)
        assertEquals(
            KeyboardWordDeletion.MaxSegmentCodePoints,
            KeyboardWordDeletion.spanBeforeCursor(longToken),
        )
    }

    @Test
    fun cappingSurrogatePairsNeverSplitsACodePoint() {
        val emojis = "\uD83D\uDE00".repeat(30)
        assertEquals(
            KeyboardWordDeletion.MaxSegmentCodePoints * 2,
            KeyboardWordDeletion.spanBeforeCursor(emojis),
        )
    }
}

package app.funput.funput.ime.editing.layout

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LetterPageReturnRuleTest(
    private val context: String?,
    private val expected: Boolean,
) {
    @Test
    fun punctuationBeforeTheSpaceLeadsBackToAWord() {
        assertEquals(expected, LetterPageReturnRule.appliesTo(context))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun cases(): List<Array<Any?>> = listOf(
            arrayOf("Chào bạn!", true),
            arrayOf("Thật sao?", true),
            arrayOf("xong.", true),
            arrayOf("ví dụ,", true),
            arrayOf("gồm:", true),
            arrayOf("chờ…", true),
            arrayOf("(có)", true),
            arrayOf("\"trích\"", true),
            arrayOf("«ừ»", true),
            arrayOf("10", false),
            arrayOf("8:30", false),
            arrayOf("xin chao", false),
            arrayOf("(", false),
            arrayOf("#", false),
            arrayOf("@", false),
            arrayOf("100%", false),
            arrayOf("a ", false),
            arrayOf("", false),
            arrayOf(null, false),
        )
    }
}

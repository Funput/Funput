package app.funput.funput.ime.editing.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SentencePunctuationRuleTest(
    private val context: String?,
    private val expected: Boolean,
) {
    @Test
    fun trailingSpaceAfterAWordOrDigitIsPunctuable() {
        assertEquals(expected, SentencePunctuationRule.appliesTo(context))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun cases(): List<Array<Any?>> = listOf(
            arrayOf("xin chao ", true),
            arrayOf("chào ", true),
            arrayOf("3 ", true),
            arrayOf("xin chao", false),
            arrayOf("xin chao  ", false),
            arrayOf("xin chao. ", false),
            arrayOf("xin chao, ", false),
            arrayOf("( ", false),
            arrayOf("\n", false),
            arrayOf("", false),
            arrayOf(null, false),
        )
    }
}

class SentencePunctuationRuleNilTest {
    @Test
    fun unknownContextNeverPunctuates() {
        assertFalse(SentencePunctuationRule.appliesTo(null))
    }
}

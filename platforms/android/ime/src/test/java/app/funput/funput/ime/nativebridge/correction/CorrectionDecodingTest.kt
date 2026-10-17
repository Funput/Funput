package app.funput.funput.ime.nativebridge.correction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrectionDecodingTest {
    @Test
    fun anEditIsItsBackspaceCountThenItsText() {
        val encoded = intArrayOf(7) + "đường ".codePoints().toArray()
        assertEquals(CorrectionEdit(7, "đường "), CorrectionEdit.decode(encoded))
    }

    @Test
    fun anEmptyEditIsANoOp() {
        assertNull(CorrectionEdit.decode(intArrayOf()))
        assertNull(CorrectionEdit.decode(null))
    }

    @Test
    fun anEmptyRankingSlotIsMinusOneNeverZero() {
        assertEquals(
            CorrectionRanking(1, null, CorrectionVerdict.APPLY),
            CorrectionRanking.decode(intArrayOf(1, -1, 0)),
        )
        assertEquals(
            CorrectionRanking(0, 1, CorrectionVerdict.AMBIGUOUS),
            CorrectionRanking.decode(intArrayOf(0, 1, 1)),
        )
    }

    @Test
    fun verdictsAreNumberedAsTheHeaderNumbersThem() {
        assertEquals(CorrectionVerdict.AS_TYPED, CorrectionRanking.decode(intArrayOf(-1, -1, 2)).verdict)
        assertEquals(CorrectionVerdict.REFUSED, CorrectionRanking.decode(intArrayOf(-1, -1, 3)).verdict)
        assertEquals(CorrectionRanking.Refused, CorrectionRanking.decode(intArrayOf(0, 0, 9)))
        assertEquals(CorrectionRanking.Refused, CorrectionRanking.decode(null))
    }

    @Test
    fun countsDecodeInTheOrderTheEngineSendsThem() {
        val counts = CorrectionCounts.decode(IntArray(10) { it + 1 })
        assertEquals(
            CorrectionCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
            counts,
        )
        assertEquals(CorrectionCounts(), CorrectionCounts.decode(intArrayOf(1, 2)))
    }
}

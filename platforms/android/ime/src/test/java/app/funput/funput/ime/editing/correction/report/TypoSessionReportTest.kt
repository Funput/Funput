package app.funput.funput.ime.editing.correction.report

import app.funput.funput.ime.editing.correction.CorrectionFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypoSessionReportTest {
    private val lines = mutableListOf<String>()

    @Test
    fun spread() {
        val spread = KeyboardTouchSpread()
        listOf(0.3f, 0.4f).forEach(spread::record)
        assertEquals(0.35, spread.mean, 1e-6)
        assertEquals(0.05, spread.standardDeviation, 1e-6)
        // √((0.09 + 0.16) / 4) = 0.25
        assertEquals(0.25, spread.perAxisSigma, 1e-6)
    }

    @Test
    fun reportedOnce() {
        val fixture = CorrectionFixture(emptyList())
        fixture.state.spread.record(0.2f)
        val report = TypoSessionReport(fixture.state, lines::add)
        report.report()
        report.report()
        assertEquals(1, lines.size)
        assertTrue(lines.single().startsWith("touches 1 sigma 0.141 mean 0.200, applied 1 reverted 0"))
    }

    @Test
    fun silentWithoutTouches() {
        TypoSessionReport(CorrectionFixture(emptyList()).state, lines::add).report()
        assertEquals(emptyList<String>(), lines)
    }
}

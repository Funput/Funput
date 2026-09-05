package app.funput.funput.ime.editing.keyevent

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyEventTextPlanTest {
    @Test
    fun emptyPreviousInsertsTheWholeReplacement() {
        assertEquals(KeyEventTextPlan(0, "a"), KeyEventTextPlan.replace("", "a"))
        assertEquals(
            listOf(KeyEventStroke.Text("a")),
            KeyEventTextPlan.replace("", "a").strokes(),
        )
    }

    @Test
    fun prefixGrowthInsertsOnlyTheSuffix() {
        assertEquals(KeyEventTextPlan(0, "h"), KeyEventTextPlan.replace("p", "ph"))
        assertEquals(KeyEventTextPlan(0, "u"), KeyEventTextPlan.replace("ph", "phu"))
    }

    @Test
    fun shrinkDeletesTheDroppedTail() {
        assertEquals(KeyEventTextPlan(1, ""), KeyEventTextPlan.replace("phu", "ph"))
        assertEquals(listOf(KeyEventStroke.Delete), KeyEventTextPlan.replace("phu", "ph").strokes())
    }

    @Test
    fun toneTransformDeletesThenInserts() {
        assertEquals(KeyEventTextPlan(1, "á"), KeyEventTextPlan.replace("a", "á"))
        assertEquals(
            listOf(KeyEventStroke.Delete, KeyEventStroke.Text("á")),
            KeyEventTextPlan.replace("a", "á").strokes(),
        )
    }

    @Test
    fun identicalBuffersEmitNothing() {
        assertEquals(emptyList<KeyEventStroke>(), KeyEventTextPlan.replace("á", "á").strokes())
    }
}

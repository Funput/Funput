package app.funput.funput.ui.playground

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaygroundGraphemeNavigatorTest {
    @Test
    fun `recognizes common Vietnamese and emoji clusters`() {
        val clusters = listOf(
            "a\u0301",
            "😀",
            "👍🏽",
            "👨‍👩‍👧‍👦",
            "🇻🇳",
            "1️⃣",
        )

        clusters.forEach { cluster ->
            assertEquals(cluster.length, PlaygroundGraphemeNavigator.next(cluster, 0))
            assertEquals(0, PlaygroundGraphemeNavigator.previous(cluster, cluster.length))
        }
    }

    @Test
    fun `pairs regional indicators from left to right`() {
        val threeIndicators = "🇻🇳🇺"
        val firstFlagEnd = "🇻🇳".length

        assertEquals(firstFlagEnd, PlaygroundGraphemeNavigator.next(threeIndicators, 0))
        assertEquals(firstFlagEnd, PlaygroundGraphemeNavigator.previous(threeIndicators, threeIndicators.length))
    }

    @Test
    fun `floor clamps offsets and avoids surrogate interiors`() {
        val emoji = "😀"

        assertEquals(0, PlaygroundGraphemeNavigator.floor(emoji, -1))
        assertEquals(0, PlaygroundGraphemeNavigator.floor(emoji, 1))
        assertEquals(emoji.length, PlaygroundGraphemeNavigator.floor(emoji, 99))
    }
}

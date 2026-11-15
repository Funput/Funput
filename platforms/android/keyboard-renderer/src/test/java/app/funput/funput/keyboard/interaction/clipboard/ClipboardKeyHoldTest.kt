package app.funput.funput.keyboard.interaction.clipboard

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardKeyHoldTest {
    @Test
    fun `hold claims after the timer and finish consumes the claim`() {
        val pending = mutableListOf<Runnable>()
        val claimed = mutableListOf<Int>()
        val hold = hold(pending, claimed)
        hold.start(1, clipboard, 10f, 10f)
        pending.single().run()
        assertEquals(listOf(1), claimed)
        assertTrue(hold.isClaimed(1))
        assertTrue(hold.finish(1))
        assertFalse(hold.isClaimed(1))
        assertFalse(hold.finish(1))
    }

    @Test
    fun `moving past touch slop abandons the hold so release can tap`() {
        val pending = mutableListOf<Runnable>()
        val claimed = mutableListOf<Int>()
        val hold = hold(pending, claimed)
        hold.start(1, clipboard, 10f, 10f)
        hold.moved(1, 30f, 10f)
        assertTrue(pending.isEmpty())
        pending.toList().forEach(Runnable::run)
        assertTrue(claimed.isEmpty())
        assertFalse(hold.finish(1))
    }

    @Test
    fun `non clipboard keys never arm the hold`() {
        val pending = mutableListOf<Runnable>()
        val hold = hold(pending, mutableListOf())
        hold.start(1, KeySpec("a", "a", KeyRole.CHARACTER), 10f, 10f)
        assertTrue(pending.isEmpty())
    }

    private fun hold(
        pending: MutableList<Runnable>,
        claimed: MutableList<Int>,
    ) = ClipboardKeyHold(
        schedule = { task, _ -> pending += task },
        cancel = { pending.remove(it) },
        touchSlop = 8f,
        onClaimed = claimed::add,
    )

    private val clipboard = KeySpec("clipboard", "", KeyRole.CLIPBOARD, accessibilityLabel = "Dán")
}

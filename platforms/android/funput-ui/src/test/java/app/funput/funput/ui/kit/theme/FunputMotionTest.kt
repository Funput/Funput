package app.funput.funput.ui.kit.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import app.funput.funput.ui.kit.tokens.MotionToken
import app.funput.funput.ui.kit.tokens.TokenCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunputMotionTest {
    @Test
    fun `a spring keeps the period of its authored duration`() {
        // Period of a unit-mass spring is 2π/√k, so √k·d must equal 2π.
        val k = stiffness(550)

        assertEquals(2 * Math.PI, Math.sqrt(k.toDouble()) * 0.55, 1e-3)
    }

    @Test
    fun `bounce maps to one minus the damping ratio`() {
        assertEquals(1f, dampingRatio(0f), 0f)
        assertEquals(0.88f, dampingRatio(0.12f), 1e-6f)
    }

    @Test
    fun `a bounce out of range cannot make the spring unstable`() {
        assertEquals(0.05f, dampingRatio(2f), 0f)
        assertEquals(1f, dampingRatio(-1f), 0f)
    }

    @Test
    fun `spring tokens become springs and eased tokens become tweens`() {
        val spring = MotionToken(TokenCurve.SPRING, durationMillis = 550, bounce = 0.12f).toSpec<Float>()
        val tween = MotionToken(TokenCurve.EASE_OUT, durationMillis = 220, bounce = 0f).toSpec<Float>()

        assertTrue(spring is SpringSpec<Float>)
        assertEquals(0.88f, (spring as SpringSpec<Float>).dampingRatio, 1e-6f)
        assertEquals(220, (tween as TweenSpec<Float>).durationMillis)
    }
}

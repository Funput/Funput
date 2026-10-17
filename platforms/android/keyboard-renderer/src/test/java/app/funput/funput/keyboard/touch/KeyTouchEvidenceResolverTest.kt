package app.funput.funput.keyboard.touch

import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.PitchX
import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.PitchY
import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.centre
import app.funput.funput.keyboard.touch.KeyTouchTestKeyboard.keyboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyTouchEvidenceResolverTest {
    private fun resolve(x: Float, y: Float, typed: String, pitchX: Float = PitchX, pitchY: Float = PitchY) =
        KeyTouchEvidenceResolver.resolve(keyboard(pitchX, pitchY), x, y, "key-$typed")

    @Test
    fun centred() {
        val (x, y) = centre(1)
        val evidence = resolve(x, y, "w")!!
        assertEquals('w'.code, evidence.typed)
        assertEquals(0f, evidence.typedDistance, 1e-4f)
    }

    @Test
    fun measuredInPitches() {
        // Half a pitch to the right and a quarter of a pitch down lands 0.559 pitches off.
        val (x, y) = centre(1)
        val evidence = resolve(x + PitchX / 2f, y + PitchY / 4f, "w")!!
        assertEquals(0.559f, evidence.typedDistance, 1e-3f)
    }

    @Test
    fun drift() {
        // Sitting between `w` and `e`, the finger was nearly as close to `e`.
        val (x, y) = centre(1)
        val evidence = resolve(x + PitchX * 0.45f, y, "w")!!
        assertEquals('e'.code, evidence.alternates.first().codePoint)
        assertEquals(0.55f, evidence.alternates.first().distance, 1e-3f)
    }

    @Test
    fun nearerNeighbour() {
        // A touch just right of centre on `s` is closer to `d` (0.9) than to `w` (1.005) or
        // `a` (1.1), and the alternates come nearest first.
        val (x, y) = centre(1, row = 1)
        val evidence = resolve(x + PitchX * 0.1f, y, "s")!!
        assertEquals(listOf('d'.code, 'w'.code, 'a'.code), evidence.alternates.map { it.codePoint })
    }

    @Test
    fun implausible() {
        // Every other key is more than 1.2 pitches from `q`'s centre, except `w` and `a`.
        val (x, y) = centre(0)
        val evidence = resolve(x, y, "q")!!
        val neighbours = evidence.alternates.map { it.codePoint }.toSet()
        assertEquals(setOf('w'.code, 'a'.code), neighbours)
        assertTrue(evidence.alternates.all { it.distance <= KeyTouchEvidence.PlausibleDistance })
    }

    @Test
    fun notACharacterKey() {
        val (x, y) = centre(3, row = 1)
        assertNull(resolve(x, y, "backspace"))
    }

    @Test
    fun noPitch() {
        val (x, y) = centre(1)
        assertNull(resolve(x, y, "w", pitchX = 0f))
    }

    @Test
    fun vniModifierIncluded() {
        // A VNI tone digit is a key the engine can swap, so it is measured like a letter.
        val (x, y) = centre(3)
        val evidence = resolve(x - PitchX * 0.4f, y, "6")!!
        assertEquals('6'.code, evidence.typed)
        assertEquals('e'.code, evidence.alternates.first().codePoint)
    }
}

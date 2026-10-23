package app.funput.funput.keyboard.interaction.backspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceRepeatPolicyTest {
    private val policy = BackspaceRepeatPolicy.Default

    @Test
    fun initialPhaseRepeatsCharactersWithMonotonicallyDecreasingDelay() {
        val steps = (0 until BackspaceRepeatPolicy.WordStageStartIndex).map(policy::step)

        assertTrue(steps.all { it.stage == BackspaceRepeatStage.Character })
        assertEquals(BackspaceRepeatPolicy.InitialCharacterIntervalMillis, steps.first().nextDelayMillis)
        assertEquals(BackspaceRepeatPolicy.FastCharacterIntervalMillis, steps.last().nextDelayMillis)
        for (index in 1 until steps.size) {
            assertTrue(steps[index].nextDelayMillis <= steps[index - 1].nextDelayMillis)
        }
    }

    @Test
    fun holdingPastCharacterThresholdEscalatesToWordStage() {
        val beforeThreshold = policy.step(BackspaceRepeatPolicy.WordStageStartIndex - 1)
        val atThreshold = policy.step(BackspaceRepeatPolicy.WordStageStartIndex)
        val wellPastThreshold = policy.step(BackspaceRepeatPolicy.WordStageStartIndex + 20)

        assertEquals(BackspaceRepeatStage.Character, beforeThreshold.stage)
        assertEquals(BackspaceRepeatStage.Word, atThreshold.stage)
        assertEquals(BackspaceRepeatPolicy.WordIntervalMillis, atThreshold.nextDelayMillis)
        assertEquals(BackspaceRepeatStage.Word, wellPastThreshold.stage)
        assertEquals(BackspaceRepeatPolicy.WordIntervalMillis, wellPastThreshold.nextDelayMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeRepeatIndexIsRejected() {
        policy.step(-1)
    }
}

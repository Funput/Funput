package app.funput.funput.keyboard.utility

import app.funput.funput.keyboard.KeyboardCallbacks
import app.funput.funput.keyboard.interaction.KeyboardUtilityActionRouter
import app.funput.funput.keyboard.interaction.KeyHapticTypeMapper
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.toKeyAction
import org.junit.Assert.*
import org.junit.Test

class MicrophoneDispatchTest {
    @Test fun microphoneUsesUtilityPathAndNeverEmitsText() {
        var count = 0
        val callbacks = KeyboardCallbacks().apply { onMicrophoneRequested = { count++ } }
        val router = KeyboardUtilityActionRouter({ error("No suggestion") }, callbacks)
        val mic = KeySpec("microphone", "", KeyRole.MICROPHONE, accessibilityLabel = "Nhập bằng giọng nói")
        router.dispatch(mic.id, mic, null) { error("No typing") }
        assertEquals(1, count)
        assertNull(mic.toKeyAction(ShiftState.OFF))
        assertEquals(KeyboardHapticType.CONTROL, KeyHapticTypeMapper.forTarget(mic, false))
    }
}

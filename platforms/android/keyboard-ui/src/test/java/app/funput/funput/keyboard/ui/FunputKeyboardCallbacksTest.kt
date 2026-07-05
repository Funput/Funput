package app.funput.funput.keyboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FunputKeyboardCallbacksTest {
    @Test
    fun `system input method request is forwarded once`() {
        val callbacks = FunputKeyboardCallbacks()
        var requests = 0
        callbacks.onInputMethodSwitchRequested = { requests += 1 }

        callbacks.dispatchInputMethodSwitchRequest()

        assertEquals(1, requests)
    }
}

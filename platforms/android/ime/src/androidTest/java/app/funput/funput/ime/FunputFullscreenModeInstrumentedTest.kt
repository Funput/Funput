package app.funput.funput.ime

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FunputFullscreenModeInstrumentedTest {
    @Test
    fun extractFullscreenModeIsAlwaysDisabled() {
        assertFalse(FunputInputMethodService().onEvaluateFullscreenMode())
    }
}

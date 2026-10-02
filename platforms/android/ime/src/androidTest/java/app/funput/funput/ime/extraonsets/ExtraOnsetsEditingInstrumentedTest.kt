package app.funput.funput.ime.extraonsets

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import app.funput.funput.ime.hardware.HardwareKeyStroke
import app.funput.funput.ime.hardware.ImeHardwareKeyHandler
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Touch and physical input share the editor's configured Rust engine. */
@RunWith(AndroidJUnit4::class)
class ExtraOnsetsEditingInstrumentedTest {
    @Test fun touchInputKeepsExtraOnsetsAcrossWordBoundaries() = onMainThread {
        ImeEditingScenario.create(extraOnsets = ExtraOnsetLetters.All).use { scenario ->
            scenario.handler.type("zoo fair jowf was food ")
            assertEquals("zô fải jờ wá food ", scenario.text)
        }
    }

    @Test fun physicalKeyEventsUseTheSameSelectedOnsets() = onMainThread {
        ImeEditingScenario.create(extraOnsets = ExtraOnsetLetters.parse("z")).use { scenario ->
            val keys = ImeHardwareKeyHandler(
                currentShift = { ShiftState.OFF },
                dispatch = scenario.handler::onKeyAction,
                finish = scenario.handler::finish,
            )
            "zoo ".forEach { char ->
                val keyCode = when (char) {
                    'z' -> KeyEvent.KEYCODE_Z
                    'o' -> KeyEvent.KEYCODE_O
                    else -> KeyEvent.KEYCODE_SPACE
                }
                val stroke = HardwareKeyStroke(keyCode = keyCode, codePoint = char.code)
                assertTrue(keys.onKeyDown(stroke))
                assertTrue(keys.onKeyUp(stroke))
            }
            assertEquals("zô ", scenario.text)
        }
    }
}

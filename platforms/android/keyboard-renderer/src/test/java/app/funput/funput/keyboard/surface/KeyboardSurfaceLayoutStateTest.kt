package app.funput.funput.keyboard.surface

import app.funput.funput.keyboard.layout.KeyboardLayouts
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSurfaceLayoutStateTest {
    @Test fun `override can be set replaced and cleared without changing resolver state`() {
        var changes = 0
        val state = KeyboardSurfaceLayoutState { changes++ }
        state.inputMethod = KeyboardInputMethod.VNI
        state.layoutOverride = KeyboardLayouts.telex
        assertEquals(KeyboardLayouts.telex, state.layout)
        state.layoutOverride = KeyboardLayouts.vni
        assertEquals(KeyboardLayouts.vni, state.layout)
        state.layoutOverride = null
        assertEquals("qwerty-vni", state.layout.id)
        assertEquals(4, changes)
    }
}

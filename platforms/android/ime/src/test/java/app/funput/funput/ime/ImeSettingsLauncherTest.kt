package app.funput.funput.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeSettingsLauncherTest {
    @Test
    fun `composition finishes before opening the app`() {
        val events = mutableListOf<String>()
        ImeSettingsLauncher(
            { events += "finish" }, { events += "launch"; true }, { events += "failure" },
        ).open()
        assertEquals(listOf("finish", "launch"), events)
    }

    @Test
    fun `missing launcher reports failure without losing composed text`() {
        val events = mutableListOf<String>()
        ImeSettingsLauncher(
            { events += "finish" }, { false }, { events += "failure" },
        ).open()
        assertEquals(listOf("finish", "failure"), events)
    }

    @Test
    fun `denied launch reports failure instead of crashing`() {
        val events = mutableListOf<String>()
        ImeSettingsLauncher(
            { events += "finish" }, { throw SecurityException("denied") }, { events += "failure" },
        ).open()
        assertEquals(listOf("finish", "failure"), events)
    }
}

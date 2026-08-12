package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImeClipboardSuppressionTest {
    @Test
    fun `success stays suppressed through expiry changes until clipboard changes`() {
        val fixture = Fixture()
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.controller.pasteCurrent()
        assertNull(fixture.controller.offer.value)

        fixture.preferences.value = ClipboardPreferences(true, ClipboardExpiry.WEEK)
        assertNull(fixture.controller.offer.value)

        fixture.gateway.snapshotValue = ClipboardSnapshot(
            ClipboardContentKind.TEXT, false, "android:v1:timestamp:2",
        )
        fixture.gateway.readResult = fixture.gateway.success(sourceToken = "android:v1:timestamp:2")
        fixture.gateway.notifyChanged()
        assertNotNull(fixture.controller.offer.value)
    }

    @Test
    fun `new input view session may offer the current clipboard again`() {
        val fixture = Fixture(sensitive = true)
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.controller.pasteCurrent()
        assertNull(fixture.controller.offer.value)

        fixture.controller.stop()
        fixture.controller.start(KeyboardEditorMode.TEXT)
        assertNotNull(fixture.controller.offer.value)
        assertEquals(1, fixture.committed.size)
    }
}

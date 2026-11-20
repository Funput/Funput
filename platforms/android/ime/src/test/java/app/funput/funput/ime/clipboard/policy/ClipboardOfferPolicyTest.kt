package app.funput.funput.ime.clipboard.policy

import app.funput.funput.ime.clipboard.model.ClipboardOfferSource
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.keyboard.model.KeyboardEditorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardOfferPolicyTest {
    @Test
    fun `offers supported clipboard kinds without exposing sensitive text`() {
        assertEquals(ClipboardOfferKind.TEXT, offer(ClipboardContentKind.TEXT).kind)
        assertEquals(ClipboardOfferKind.LINK, offer(ClipboardContentKind.LINK).kind)
        assertEquals(ClipboardOfferKind.SENSITIVE, offer(
            ClipboardContentKind.TEXT,
            sensitive = true,
        ).kind)
    }

    @Test
    fun `blocks disabled inactive unsupported and pasted clips`() {
        assertNull(policy(null))
        assertNull(policy(snapshot(), enabled = false))
        assertNull(policy(snapshot(), active = false))
        assertNull(policy(snapshot(), lastPasted = Token))
        assertEquals(ClipboardOfferKind.TEXT, policy(snapshot(token = null))?.kind)
    }

    @Test
    fun `blocks editors without the utility toolbar`() {
        val blocked = listOf(
            KeyboardEditorMode.PASSWORD,
            KeyboardEditorMode.PIN,
            KeyboardEditorMode.PHONE,
            KeyboardEditorMode.NUMBER,
            KeyboardEditorMode.NUMBER_DECIMAL,
        )
        blocked.forEach { mode -> assertNull(policy(snapshot(), mode = mode)) }
        assertEquals(ClipboardOfferKind.TEXT, policy(snapshot(), mode = KeyboardEditorMode.TEXT)?.kind)
    }

    @Test
    fun `previews only text the history already holds`() {
        assertEquals("xin chào", policy(snapshot(), held = " xin\nchào ")?.preview)
        assertNull(policy(snapshot())?.preview)
    }

    @Test
    fun `never previews a sensitive clip or when recording copies is off`() {
        assertNull(policy(snapshot(sensitive = true), held = "mật khẩu")?.preview)
        assertNull(policy(snapshot(), held = "xin chào", showsPreview = false)?.preview)
    }

    private fun offer(kind: ClipboardContentKind, sensitive: Boolean = false) =
        requireNotNull(policy(snapshot(kind, sensitive)))

    private fun policy(
        snapshot: ClipboardSnapshot?,
        enabled: Boolean = true,
        active: Boolean = true,
        lastPasted: String? = null,
        mode: KeyboardEditorMode = KeyboardEditorMode.TEXT,
        held: String? = null,
        showsPreview: Boolean = true,
    ) = ClipboardOfferPolicy.offer(
        snapshot,
        ClipboardOfferSource(lastPasted, held),
        ClipboardOfferPolicy.Context(enabled, active, mode, showsPreview),
    )

    private fun snapshot(
        kind: ClipboardContentKind? = ClipboardContentKind.TEXT,
        sensitive: Boolean = false,
        token: String? = Token,
    ) = ClipboardSnapshot(kind, sensitive, token)

    private companion object { const val Token = "android:v1:timestamp:1" }
}

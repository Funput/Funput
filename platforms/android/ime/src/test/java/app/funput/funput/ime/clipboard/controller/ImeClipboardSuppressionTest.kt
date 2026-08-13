package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardFallbackTokenCache
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.ime.clipboard.platform.ClipboardSourceToken
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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

    @Test
    fun `fallback hash suppresses saved clipboard in the next session`() {
        val gateway = FallbackTokenGateway()
        val directory = Files.createTempDirectory("clipboard-fallback").toFile()
        val controller = ImeClipboardController(
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            MutableStateFlow(ClipboardPreferences.Default), gateway,
            { ClipboardHistoryStore(directory, it) }, {}, {}, Dispatchers.Unconfined,
        )
        controller.start(KeyboardEditorMode.TEXT)
        assertNotNull(controller.offer.value)
        controller.pasteCurrent()
        controller.stop()

        controller.start(KeyboardEditorMode.TEXT)

        assertNull(controller.offer.value)
        controller.close()
    }
}

private class FallbackTokenGateway : ClipboardGateway {
    private val cache = ClipboardFallbackTokenCache()
    private val raw = ClipboardSnapshot(ClipboardContentKind.TEXT, false, null)
    override fun snapshot() = cache.enrich(raw)
    override fun observe(onChanged: () -> Unit) = ClipboardObservation {}
    override fun readText(maxLength: Int): ClipboardReadResult {
        val token = ClipboardSourceToken.fromText(Text)
        cache.remember(raw, token)
        return ClipboardReadResult.Success(Text, ClipboardContentKind.TEXT, false, token)
    }
    private companion object { const val Text = "clipboard" }
}

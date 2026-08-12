package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardEntry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.model.KeyboardEditorMode
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeClipboardHardeningTest {
    @Test
    fun `metadata change with the same token cannot commit`() {
        val fixture = Fixture()
        fixture.controller.start(KeyboardEditorMode.TEXT)
        fixture.gateway.readResult = fixture.gateway.success(sensitive = true)
        var result: ClipboardPasteResult? = null

        fixture.controller.pasteCurrent { result = it }

        assertEquals(ClipboardPasteResult.CHANGED, result)
        assertTrue(fixture.committed.isEmpty())
        fixture.controller.close()
    }

    @Test
    fun `paste still commits when persistence is unavailable`() {
        val parent = Files.createTempDirectory("clipboard-paste-failure").toFile()
        val blocked = parent.resolve("blocked").apply { writeText("file") }
        val gateway = FakeClipboardGateway("kept", false)
        val committed = mutableListOf<String>()
        val controller = ImeClipboardController(
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            MutableStateFlow(ClipboardPreferences.Default), gateway,
            { ClipboardHistoryStore(blocked, it) }, committed::add, {}, Dispatchers.Unconfined,
        )
        controller.start(KeyboardEditorMode.TEXT)

        controller.pasteCurrent()

        assertEquals(listOf("kept"), committed)
        assertTrue(ClipboardHistoryStore(blocked).load().isEmpty())
        controller.close()
    }

    @Test
    fun `callback retained by a closed observation cannot restore offer`() {
        val gateway = RetainedCallbackGateway()
        val directory = Files.createTempDirectory("clipboard-stale-listener").toFile()
        val controller = ImeClipboardController(
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            MutableStateFlow(ClipboardPreferences.Default), gateway,
            { ClipboardHistoryStore(directory, it) }, {}, {}, Dispatchers.Unconfined,
        )
        controller.start(KeyboardEditorMode.TEXT)
        controller.stop()

        gateway.fireRetainedCallback()

        assertNull(controller.offer.value)
        controller.close()
    }

    @Test
    fun `failed clear keeps panel state and does not report success`() {
        val directory = Files.createTempDirectory("clipboard-clear-failure").toFile()
        val entry = ClipboardEntry(
            text = "old", sourceToken = "old",
            capturedAt = java.time.Instant.ofEpochMilli(System.currentTimeMillis()),
        )
        ClipboardHistoryStore(directory).record(entry)
        val failing = ClipboardHistoryStore(directory, app.funput.funput.ime.clipboard.model.ClipboardExpiry.HOUR) {
                _, _ -> throw IOException("failed")
        }
        var cleared = 0
        val controller = ImeClipboardHistoryController(
            CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            MutableStateFlow(ClipboardPreferences.Default), { failing }, {}, {}, {},
            { cleared += 1 }, Dispatchers.Unconfined,
        )
        controller.start(KeyboardEditorMode.TEXT)
        controller.open()

        controller.clear()

        assertEquals(listOf(entry), controller.state.value.entries)
        assertEquals(0, cleared)
        controller.close()
    }
}

private class RetainedCallbackGateway : ClipboardGateway {
    private var callback: (() -> Unit)? = null
    override fun snapshot() = ClipboardSnapshot(ClipboardContentKind.TEXT, false, Token)
    override fun readText(maxLength: Int) = ClipboardReadResult.Empty
    override fun observe(onChanged: () -> Unit) = ClipboardObservation {}.also { callback = onChanged }
    fun fireRetainedCallback() { callback?.invoke() }
    private companion object { const val Token = "android:v1:timestamp:1" }
}

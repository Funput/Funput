package app.funput.funput.ime.editing.voice

import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.AuthoredSuggestionUpdate
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import org.junit.Assert.*
import org.junit.Test

class VoiceTextCommitTest {
    @Test fun voiceCommitsExactTextWithoutEngineOrAuthoredSuggestionTracking() {
        val f = VoiceCommitFixture()
        val transcript = "aa dd ww as a1,  Bạn🙂!"
        assertTrue(f.handler.commitVoiceText(transcript))
        assertEquals(listOf(transcript), f.connection.commits)
        assertEquals(0, f.engine.calls)
        assertEquals(AuthoredSuggestionUpdate.Empty, f.handler.takeSuggestionUpdate())
    }

    @Test fun hostFalseIsReturnedAndNeverRetried() {
        val f = VoiceCommitFixture()
        f.connection.accepts = false
        assertFalse(f.handler.commitVoiceText("voice"))
        assertEquals(1, f.connection.attempts)
        assertTrue(f.connection.commits.isEmpty())
        assertEquals(0, f.engine.calls)
        assertEquals(AuthoredSuggestionUpdate.Empty, f.handler.takeSuggestionUpdate())
    }

    @Test fun absentConnectionAndEmptyTextDoNotWrite() {
        val f = VoiceCommitFixture()
        assertFalse(f.handler.commitVoiceText(""))
        f.connected = false
        assertFalse(f.handler.commitVoiceText("voice"))
        assertEquals(0, f.connection.attempts)
        assertEquals(0, f.engine.calls)
    }

    @Test fun emojiAndClipboardStillFinishBeforeExactExternalCommit() {
        val f = VoiceCommitFixture()
        f.handler.onEmojiSelected(" 🙂 ")
        f.handler.onClipboardSelected("  aa dd\n ")
        assertEquals(listOf(" 🙂 ", "  aa dd\n "), f.connection.commits)
        assertTrue(f.engine.calls > 0)
        assertEquals(AuthoredSuggestionUpdate.Empty, f.handler.takeSuggestionUpdate())
    }
}

private class VoiceCommitFixture {
    val engine = VoiceTestEngine()
    val connection = VoiceTestConnection()
    var connected = true
    val handler = ImeKeyActionHandler(AndroidCompositionSession(engine), InputConnectionEditor(),
        { if (connected) connection.proxy else null }, { ImeEditCommand.CommitText("\n") })
}

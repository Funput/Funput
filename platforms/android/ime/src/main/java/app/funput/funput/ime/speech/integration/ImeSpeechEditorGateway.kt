package app.funput.funput.ime.speech.integration

import android.Manifest
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.PowerManager
import android.view.inputmethod.ExtractedTextRequest
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.speech.editor.SpeechAnchorEnvironment
import app.funput.funput.ime.speech.editor.SpeechAnchorResolver
import app.funput.funput.ime.speech.editor.SpeechEditorTracker
import app.funput.funput.ime.speech.editor.SpeechSelection
import app.funput.funput.ime.speech.session.SpeechCancellation
import app.funput.funput.ime.speech.session.SpeechEditorAnchor
import app.funput.funput.ime.speech.session.SpeechEditorGateway

/** Current connection is resolved per operation; wrapper identity never replaces the editor stamp. */
internal class ImeSpeechEditorGateway(
    private val service: InputMethodService,
    private val session: ImeEditingSession,
    private val tracker: SpeechEditorTracker,
) : SpeechEditorGateway, SpeechAnchorEnvironment {
    var visible = false
    private val resolver = SpeechAnchorResolver(tracker, this)

    fun invalidate() = resolver.cancel()
    fun selectionUpdated(): Boolean = resolver.selectionUpdated()

    fun permitted(): Boolean = service.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    override fun available(): Boolean = foreground() && permitted()

    fun foreground(): Boolean = visible && service.isInputViewShown &&
        service.currentInputConnection != null &&
        service.getSystemService(PowerManager::class.java).isInteractive &&
        !service.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    override fun finishComposition() {
        session.actionHandler.finish()
        session.suggestionService.finish()
    }

    override fun selection(): SpeechSelection? = service.currentInputConnection
        ?.getExtractedText(ExtractedTextRequest(), 0)?.let {
            if (it.selectionStart < 0 || it.selectionEnd < 0) null
            else SpeechSelection(it.startOffset + it.selectionStart, it.startOffset + it.selectionEnd)
        }

    override fun prepareAnchor(listener: (SpeechEditorAnchor?) -> Unit): SpeechCancellation = resolver.prepare(listener)

    override fun isValid(anchor: SpeechEditorAnchor): Boolean {
        if (!available() || !tracker.matches(anchor)) return false
        val observed = selection()
        if (observed != null) tracker.selectionChanged(observed.start, observed.end)
        return available() && tracker.matches(anchor)
    }

    override fun commit(anchor: SpeechEditorAnchor, text: String): Boolean {
        if (!isValid(anchor)) return false
        val committed = session.actionHandler.commitVoiceText(text)
        if (committed) session.editorRuntime.updateCapitalization(preserveCapsLock = false)
        return committed
    }
}

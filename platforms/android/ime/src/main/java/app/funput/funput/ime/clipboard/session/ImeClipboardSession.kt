package app.funput.funput.ime.clipboard.session

import android.content.Context
import app.funput.funput.ime.clipboard.capture.ClipboardSystemCapture
import app.funput.funput.ime.clipboard.controller.ImeClipboardController
import app.funput.funput.ime.clipboard.controller.ImeClipboardHistoryController
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.AndroidClipboardGateway
import app.funput.funput.ime.clipboard.ui.ImeClipboardUiBinding
import app.funput.funput.ime.settings.ClipboardSettings
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope

/**
 * Clipboard offer, history panel, optional system-copy capture, and keyboard binding.
 * Capture lives for the IME session; offer/panel still follow input-view start/stop.
 */
internal class ImeClipboardSession(
    context: Context,
    scope: CoroutineScope,
    commitText: (String) -> Unit,
    afterCommit: () -> Unit,
    preparePanel: () -> Unit,
) {
    private val preferences = ClipboardSettings(context).preferences
    private val gateway = AndroidClipboardGateway(context)
    private val storeFactory = { expiry: ClipboardExpiry -> ClipboardHistoryStore.from(context, expiry) }
    private val controller = ImeClipboardController(
        parentScope = scope,
        preferences = preferences,
        gateway = gateway,
        storeFactory = storeFactory,
        commitText = commitText,
        afterCommit = afterCommit,
    )
    private val historyController = ImeClipboardHistoryController(
        parentScope = scope,
        preferences = preferences,
        storeFactory = storeFactory,
        commitText = commitText,
        afterCommit = afterCommit,
        preparePanel = preparePanel,
        onCleared = controller::historyCleared,
    )
    private val systemCapture = ClipboardSystemCapture(
        parentScope = scope,
        preferences = preferences,
        gateway = gateway,
        storeFactory = storeFactory,
        onRecorded = historyController::entriesChanged,
    )
    private val uiBinding = ImeClipboardUiBinding(context, scope, controller, historyController)

    fun start(editorMode: KeyboardEditorMode) {
        controller.start(editorMode)
        historyController.start(editorMode)
        systemCapture.start(editorMode)
    }

    fun stop() {
        controller.stop()
        historyController.stop()
        systemCapture.stop()
    }

    fun attach(view: FunputKeyboardView) = uiBinding.attach(view)

    fun close() {
        uiBinding.close()
        systemCapture.close()
        historyController.close()
        controller.close()
    }
}

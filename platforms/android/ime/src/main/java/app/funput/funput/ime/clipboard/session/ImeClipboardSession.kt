package app.funput.funput.ime.clipboard.session

import android.content.Context
import app.funput.funput.ime.clipboard.controller.ImeClipboardController
import app.funput.funput.ime.clipboard.controller.ImeClipboardHistoryController
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.AndroidClipboardGateway
import app.funput.funput.ime.clipboard.ui.ImeClipboardUiBinding
import app.funput.funput.ime.settings.ClipboardSettings
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope

/**
 * The clipboard offer, the clipboard history and their binding to the keyboard view,
 * started, stopped and closed together as the IME's input comes and goes.
 */
internal class ImeClipboardSession(
    context: Context,
    scope: CoroutineScope,
    commitText: (String) -> Unit,
    afterCommit: () -> Unit,
    preparePanel: () -> Unit,
) {
    private val preferences = ClipboardSettings(context).preferences
    private val controller = ImeClipboardController(
        parentScope = scope,
        preferences = preferences,
        gateway = AndroidClipboardGateway(context),
        storeFactory = { expiry -> ClipboardHistoryStore.from(context, expiry) },
        commitText = commitText,
        afterCommit = afterCommit,
    )
    private val historyController = ImeClipboardHistoryController(
        parentScope = scope,
        preferences = preferences,
        storeFactory = { expiry -> ClipboardHistoryStore.from(context, expiry) },
        commitText = commitText,
        afterCommit = afterCommit,
        preparePanel = preparePanel,
        onCleared = controller::historyCleared,
    )
    private val uiBinding = ImeClipboardUiBinding(context, scope, controller, historyController)

    fun start(editorMode: KeyboardEditorMode) {
        controller.start(editorMode)
        historyController.start(editorMode)
    }

    fun stop() {
        controller.stop()
        historyController.stop()
    }

    fun attach(view: FunputKeyboardView) = uiBinding.attach(view)

    fun close() {
        uiBinding.close()
        historyController.close()
        controller.close()
    }
}

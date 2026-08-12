package app.funput.funput.ime.clipboard.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import app.funput.funput.ime.R
import app.funput.funput.ime.clipboard.controller.ClipboardPasteResult
import app.funput.funput.ime.clipboard.controller.ImeClipboardController
import app.funput.funput.ime.clipboard.policy.ClipboardOffer
import app.funput.funput.ime.clipboard.policy.ClipboardOfferKind
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class ImeClipboardUiBinding(
    context: Context,
    scope: CoroutineScope,
    private val controller: ImeClipboardController,
) {
    private val appContext = context.applicationContext
    private var view: FunputKeyboardView? = null
    private val observation: Job = scope.launch {
        controller.offer.collectLatest { offer -> view?.clipboardHint = offer.toHint() }
    }

    fun attach(value: FunputKeyboardView) {
        detachView()
        view = value
        value.clipboardHint = controller.offer.value.toHint()
        value.callbacks.onClipboardPasteRequested = {
            controller.pasteCurrent(::showResult)
        }
    }

    fun close() {
        detachView()
        observation.cancel()
    }

    private fun detachView() {
        view?.callbacks?.onClipboardPasteRequested = null
        view?.clipboardHint = null
        view = null
    }

    private fun showResult(result: ClipboardPasteResult) {
        val message = result.messageResource() ?: return
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }
}

internal fun ClipboardOffer?.toHint(): KeyboardClipboardHint? = when (this?.kind) {
    ClipboardOfferKind.TEXT -> KeyboardClipboardHint.TEXT
    ClipboardOfferKind.LINK -> KeyboardClipboardHint.LINK
    ClipboardOfferKind.SENSITIVE -> KeyboardClipboardHint.SENSITIVE
    null -> null
}

@StringRes
internal fun ClipboardPasteResult.messageResource(): Int? = when (this) {
    ClipboardPasteResult.TOO_LARGE -> R.string.clipboard_paste_too_large
    ClipboardPasteResult.EMPTY,
    ClipboardPasteResult.UNSUPPORTED,
    ClipboardPasteResult.UNAVAILABLE,
    -> R.string.clipboard_paste_failed
    ClipboardPasteResult.PASTED,
    ClipboardPasteResult.BLOCKED,
    ClipboardPasteResult.BUSY,
    ClipboardPasteResult.CHANGED,
    -> null
}

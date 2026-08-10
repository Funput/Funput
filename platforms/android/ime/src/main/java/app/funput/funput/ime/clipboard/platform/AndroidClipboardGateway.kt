package app.funput.funput.ime.clipboard.platform

import android.content.ClipboardManager
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidClipboardGateway(context: Context) : ClipboardGateway {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(ClipboardManager::class.java)

    override fun snapshot(): ClipboardSnapshot? = runCatching {
        if (!manager.hasPrimaryClip()) return null
        manager.primaryClipDescription?.clipboardSnapshot()
    }.getOrNull()

    override fun readText(maxLength: Int): ClipboardReadResult = runCatching {
        val clip = manager.primaryClip ?: return ClipboardReadResult.Empty
        clip.readClipboardText(appContext, maxLength)
    }.getOrElse { ClipboardReadResult.Unavailable }

    override fun observe(onChanged: () -> Unit): ClipboardObservation {
        val listener = ClipboardManager.OnPrimaryClipChangedListener(onChanged)
        val registered = runCatching { manager.addPrimaryClipChangedListener(listener) }.isSuccess
        val closed = AtomicBoolean(false)
        return ClipboardObservation {
            if (registered && closed.compareAndSet(false, true)) {
                runCatching { manager.removePrimaryClipChangedListener(listener) }
            }
        }
    }

}

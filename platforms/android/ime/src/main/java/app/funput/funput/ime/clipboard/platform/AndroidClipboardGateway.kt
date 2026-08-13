package app.funput.funput.ime.clipboard.platform

import android.content.ClipboardManager
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidClipboardGateway(context: Context) : ClipboardGateway {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(ClipboardManager::class.java)
    private val fallbackTokens = ClipboardFallbackTokenCache()

    override fun snapshot(): ClipboardSnapshot? = runCatching {
        if (!manager.hasPrimaryClip()) return null
        manager.primaryClipDescription?.clipboardSnapshot()?.let(fallbackTokens::enrich)
    }.getOrNull()

    override fun readText(maxLength: Int): ClipboardReadResult = runCatching {
        val clip = manager.primaryClip ?: return ClipboardReadResult.Empty
        clip.readClipboardText(appContext, maxLength).also { result ->
            if (result is ClipboardReadResult.Success) {
                fallbackTokens.remember(clip.description.clipboardSnapshot(), result.sourceToken)
            }
        }
    }.getOrElse { ClipboardReadResult.Unavailable }

    override fun observe(onChanged: () -> Unit): ClipboardObservation {
        val closed = AtomicBoolean(false)
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            fallbackTokens.invalidate()
            if (!closed.get()) onChanged()
        }
        val registered = runCatching { manager.addPrimaryClipChangedListener(listener) }.isSuccess
        return ClipboardObservation {
            if (registered && closed.compareAndSet(false, true)) {
                runCatching { manager.removePrimaryClipChangedListener(listener) }
            }
        }
    }

}

package app.funput.funput.keyboard.ui.kaomoji.catalog

import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

internal object KaomojiCatalogLoader {
    private const val AssetName = "KaomojiCatalog.json"
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "FunputKaomojiCatalog").apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }
    }
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var cached: KaomojiCatalog? = null
    private var loading = false
    private val callbacks = mutableListOf<(KaomojiCatalog) -> Unit>()

    fun load(context: Context, completion: (KaomojiCatalog) -> Unit) {
        cached?.let(completion) ?: enqueue(context.applicationContext, completion)
    }

    private fun enqueue(context: Context, completion: (KaomojiCatalog) -> Unit) {
        synchronized(this) {
            cached?.let { ready -> main.post { completion(ready) }; return }
            callbacks += completion
            if (loading) return
            loading = true
            executor.execute { decode(context) }
        }
    }

    private fun decode(context: Context) {
        val decoded = runCatching {
            context.assets.open(AssetName).bufferedReader().use { KaomojiCatalogDecoder.decode(it.readText()) }
        }.getOrDefault(KaomojiCatalog.Empty)
        val paint = Paint()
        val supported = KaomojiGlyphSupport(context).filter(decoded, paint::hasGlyph)
        synchronized(this) {
            cached = supported
            loading = false
            val pending = callbacks.toList()
            callbacks.clear()
            main.post { pending.forEach { it(supported) } }
        }
    }
}

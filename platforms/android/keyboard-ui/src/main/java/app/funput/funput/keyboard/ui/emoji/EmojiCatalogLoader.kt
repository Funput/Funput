package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

internal object EmojiCatalogLoader {
    private const val AssetName = "EmojiCatalog.json"
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "FunputEmojiCatalog").apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }
    }
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var cached: EmojiCatalogContent? = null
    private var loading = false
    private val callbacks = mutableListOf<(EmojiCatalogContent) -> Unit>()

    fun load(context: Context, completion: (EmojiCatalogContent) -> Unit) {
        cached?.let(completion) ?: enqueue(context, completion)
    }

    fun preload(context: Context) = enqueue(context, null)

    private fun enqueue(context: Context, completion: ((EmojiCatalogContent) -> Unit)?) {
        synchronized(this) {
            cached?.let { ready ->
                completion?.let { main.post { it(ready) } }
                return
            }
            completion?.let(callbacks::add)
            if (loading) return
            loading = true
            decode(context.applicationContext)
        }
    }

    private fun decode(context: Context) = executor.execute {
        val decoded = runCatching {
            context.assets.open(AssetName).bufferedReader().use { reader ->
                EmojiCatalogDecoder.decode(reader.readText())
            }
        }.getOrDefault(EmojiCatalog.Empty)
        val paint = Paint()
        val supported = EmojiGlyphSupportCache(context).filter(decoded, paint::hasGlyph)
        val content = EmojiCatalogContent(supported, EmojiSearchIndex(supported.emojis))
        synchronized(this) {
            cached = content
            loading = false
            val pending = callbacks.toList()
            callbacks.clear()
            main.post { pending.forEach { it(content) } }
        }
    }
}

internal data class EmojiCatalogContent(
    val catalog: EmojiCatalog,
    val searchIndex: EmojiSearchIndex,
)

package app.funput.funput.ui.news.data

import java.io.File

/**
 * The last feed that downloaded, kept so the tab opens with posts even offline. It lives in the
 * cache directory: the system may clear it, and the next download simply writes it again.
 */
internal class NewsFeedCache(private val file: File) {
    /** The saved feed and when it was saved, or null when there is none to read. */
    fun read(): Saved? = runCatching {
        if (!file.isFile) null else Saved(file.readText(Charsets.UTF_8), file.lastModified())
    }.getOrNull()

    /** Replaces the saved feed with [json], written whole or not at all. */
    fun write(json: String) {
        val partial = File(file.parentFile, file.name + ".tmp")
        partial.writeText(json, Charsets.UTF_8)
        if (!partial.renameTo(file)) {
            partial.copyTo(file, overwrite = true)
            partial.delete()
        }
    }

    class Saved(val json: String, val savedAtMillis: Long)

    companion object {
        const val FileName = "news-feed.json"
    }
}

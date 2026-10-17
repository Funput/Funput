package app.funput.funput.ui.news.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.ui.news.model.NewsArticle
import app.funput.funput.ui.news.model.NewsFeed
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the news list stands: nothing tried yet, downloading, shown, or the download failed. */
internal enum class NewsPhase { IDLE, LOADING, LOADED, FAILED }

/**
 * The news tab's posts: the saved feed first, then a download when it is older than [freshForMillis].
 * A failed download keeps whatever is already shown, so going offline never empties the tab.
 */
internal class NewsModel(
    private val fetcher: NewsFeedFetching,
    private val cache: NewsFeedCache,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
    private val freshForMillis: Long = SixHoursMillis,
) {
    var articles by mutableStateOf<List<NewsArticle>>(emptyList())
        private set
    var phase by mutableStateOf(NewsPhase.IDLE)
        private set
    var updatedAtMillis by mutableStateOf<Long?>(null)
        private set

    /** Shows the saved feed, then downloads only when it is missing or stale. */
    fun loadIfNeeded() {
        if (phase == NewsPhase.LOADING) return
        scope.launch {
            if (articles.isEmpty()) {
                val saved = withContext(io) { cache.read() }
                val decoded = saved?.let { decodeOrNull(it.json) }
                if (saved != null && decoded != null) {
                    articles = decoded
                    updatedAtMillis = saved.savedAtMillis
                    phase = NewsPhase.LOADED
                }
            }
            val updated = updatedAtMillis
            if (updated == null || now() - updated >= freshForMillis) refresh()
        }
    }

    /** Downloads the feed now; a second call while one is running does nothing. */
    fun refresh() {
        if (phase == NewsPhase.LOADING) return
        phase = NewsPhase.LOADING
        scope.launch {
            try {
                val json = withContext(io) { fetcher.fetch() }
                val fresh = NewsFeed.decode(json)
                withContext(io) { runCatching { cache.write(json) } }
                articles = fresh
                updatedAtMillis = now()
                phase = NewsPhase.LOADED
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                phase = NewsPhase.FAILED
            }
        }
    }

    private fun decodeOrNull(json: String): List<NewsArticle>? = runCatching { NewsFeed.decode(json) }.getOrNull()

    companion object {
        const val SixHoursMillis: Long = 6 * 60 * 60 * 1000L
    }
}

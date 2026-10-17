package app.funput.funput.ui.news

import app.funput.funput.ui.news.data.NewsFeedCache
import app.funput.funput.ui.news.data.NewsFeedFetching
import app.funput.funput.ui.news.data.NewsModel
import app.funput.funput.ui.news.data.NewsPhase
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The saved feed shows first; the network is asked only when it is stale, and a failure keeps it. */
class NewsModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var calls = 0
    private var now = 1_000_000_000L

    private fun model(cacheFile: File, answer: () -> String) = NewsModel(
        fetcher = NewsFeedFetching { calls += 1; answer() },
        cache = NewsFeedCache(cacheFile),
        scope = CoroutineScope(Dispatchers.Unconfined),
        io = Dispatchers.Unconfined,
        now = { now },
    )

    private fun savedFeed(ageMillis: Long): File = folder.newFile(NewsFeedCache.FileName).apply {
        writeText(NewsFixture.Feed)
        setLastModified(now - ageMillis)
    }

    @Test
    fun `a fresh saved feed shows without touching the network`() {
        val model = model(savedFeed(ageMillis = 60_000)) { error("no network expected") }

        model.loadIfNeeded()

        assertEquals(0, calls)
        assertEquals(NewsPhase.LOADED, model.phase)
        assertEquals(2, model.articles.size)
    }

    @Test
    fun `a stale saved feed stays on screen when the download fails`() {
        val model = model(savedFeed(ageMillis = NewsModel.SixHoursMillis + 1)) { throw IOException("offline") }

        model.loadIfNeeded()

        assertEquals(1, calls)
        assertEquals(NewsPhase.FAILED, model.phase)
        assertEquals(listOf("new-mac", "old-linux"), model.articles.map { it.id })
    }

    @Test
    fun `a download is shown and saved for next time`() {
        val file = File(folder.root, NewsFeedCache.FileName)
        val model = model(file) { NewsFixture.Feed }

        model.loadIfNeeded()

        assertEquals(NewsPhase.LOADED, model.phase)
        assertEquals(2, model.articles.size)
        assertEquals(NewsFixture.Feed, file.readText())
    }

    @Test
    fun `a refresh while one is running asks the network once`() {
        lateinit var model: NewsModel
        model = model(File(folder.root, NewsFeedCache.FileName)) {
            model.refresh()
            NewsFixture.Feed
        }

        model.refresh()

        assertEquals(1, calls)
        assertEquals(NewsPhase.LOADED, model.phase)
    }
}

package app.funput.funput.ui.news

import app.funput.funput.ui.news.model.NewsFeed
import app.funput.funput.ui.news.model.NewsGuide
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The blog feed reads into posts newest first, and one broken entry does not spoil the rest. */
class NewsFeedTest {
    private val articles = NewsFeed.decode(NewsFixture.Feed)

    @Test
    fun `posts come newest first and broken entries are skipped`() {
        assertEquals(listOf("new-mac", "old-linux"), articles.map { it.id })
    }

    @Test
    fun `dates read with and without fractions of a second`() {
        assertEquals(Instant.parse("2026-10-06T00:00:00Z"), articles[0].published)
        assertEquals(Instant.parse("2026-09-29T01:05:00Z"), articles[1].published)
    }

    @Test
    fun `tags say whether a post is a guide and which platform it is about`() {
        assertTrue(articles[0].isGuide)
        assertEquals("macOS", articles[0].platform)
        assertFalse(articles[1].isGuide)
        assertEquals("Linux", articles[1].platform)
    }

    @Test
    fun `a missing summary or tag list reads as empty`() {
        val item = """{"id":"a","url":"https://funput.app/a","title":"A","date_published":"2026-01-01T00:00:00Z"}"""
        val bare = NewsFeed.decode("""{"items":[$item]}""").single()

        assertEquals("", bare.summary)
        assertEquals(emptyList<String>(), bare.tags)
        assertNull(bare.platform)
    }

    @Test
    fun `pinned guides are distinct https pages on the docs site`() {
        assertEquals(NewsGuide.entries.size, NewsGuide.entries.map { it.url }.distinct().size)
        NewsGuide.entries.forEach { guide -> assertTrue(guide.url, guide.url.startsWith("https://docs.funput.app/")) }
    }
}

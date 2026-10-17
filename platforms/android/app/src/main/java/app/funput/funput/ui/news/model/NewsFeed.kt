package app.funput.funput.ui.news.model

import java.time.Instant
import java.time.OffsetDateTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads a JSON Feed 1.1 document (https://jsonfeed.org/version/1.1) into posts, newest first.
 *
 * A post the site gets wrong (no id, no date) is skipped rather than failing the whole feed: one
 * broken entry should not empty the tab.
 */
internal object NewsFeed {
    /** Throws [org.json.JSONException] when [json] is not a feed at all. */
    fun decode(json: String): List<NewsArticle> {
        val items = JSONObject(json).getJSONArray("items")
        return (0 until items.length())
            .mapNotNull { index -> items.optJSONObject(index)?.let(::articleOrNull) }
            .sortedByDescending { article -> article.published }
    }

    private fun articleOrNull(item: JSONObject): NewsArticle? {
        val id = item.optString("id").takeIf { it.isNotBlank() } ?: return null
        val url = item.optString("url").takeIf { it.startsWith("https://") } ?: return null
        val title = item.optString("title").takeIf { it.isNotBlank() } ?: return null
        val published = parseDate(item.optString("date_published")) ?: return null
        return NewsArticle(
            id = id,
            url = url,
            title = title,
            summary = item.optString("summary"),
            tags = item.optJSONArray("tags").strings(),
            published = published,
        )
    }

    /** ISO 8601 with an offset, with or without fractions of a second. */
    private fun parseDate(value: String): Instant? =
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { index -> optString(index).ifBlank { null } }
}

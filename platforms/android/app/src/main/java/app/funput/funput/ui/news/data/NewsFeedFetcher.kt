package app.funput.funput.ui.news.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads the blog feed; tests and previews swap in their own. */
internal fun interface NewsFeedFetching {
    /** The feed's JSON text. Throws [IOException] when it cannot be had. */
    fun fetch(): String
}

/**
 * The funput.app blog feed over HTTPS, with nothing but the platform's own HTTP client: a single
 * GET a few times a day does not justify a networking library in the keyboard's APK.
 */
internal class HttpNewsFeedFetcher(private val url: String = FeedUrl) : NewsFeedFetching {
    override fun fetch(): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMs
            connection.readTimeout = TimeoutMs
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/feed+json, application/json")
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("News feed answered HTTP $status")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val FeedUrl = "https://funput.app/blog/feed.json"
        private const val TimeoutMs = 15_000
    }
}

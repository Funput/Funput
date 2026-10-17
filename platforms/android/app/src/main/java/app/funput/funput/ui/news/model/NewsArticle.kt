package app.funput.funput.ui.news.model

import java.time.Instant

/** One post of the funput.app blog, as the JSON Feed describes it. */
internal data class NewsArticle(
    val id: String,
    val url: String,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val published: Instant,
) {
    /** A how-to post, marked on the site with the "huong-dan" tag. */
    val isGuide: Boolean get() = GuideTag in tags

    /** The platform the post is about, named the way the app names it, or null for a general post. */
    val platform: String? get() = tags.firstNotNullOfOrNull { tag -> PlatformNames[tag] }

    private companion object {
        const val GuideTag = "huong-dan"
        val PlatformNames = mapOf(
            "ios" to "iOS",
            "macos" to "macOS",
            "windows" to "Windows",
            "linux" to "Linux",
            "android" to "Android",
        )
    }
}

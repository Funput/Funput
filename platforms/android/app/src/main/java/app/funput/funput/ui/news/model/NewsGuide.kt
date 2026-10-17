package app.funput.funput.ui.news.model

import androidx.annotation.StringRes
import app.funput.funput.R

/** A guide pinned above the posts: the two pages new users look for first. */
internal enum class NewsGuide(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    val url: String,
) {
    SETUP(
        R.string.news_guide_setup_title,
        R.string.news_guide_setup_summary,
        "https://docs.funput.app/docs/install/android/",
    ),
    TYPING(
        R.string.news_guide_typing_title,
        R.string.news_guide_typing_summary,
        "https://docs.funput.app/docs/input-methods/vietnamese/",
    ),
}

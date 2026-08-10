package app.funput.funput.ui.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R

/** One outbound link on the about screen. URLs live in resources so they sit next to their labels. */
internal data class AboutLink(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    @param:DrawableRes val icon: Int,
    @param:StringRes val url: Int,
)

/**
 * The same three groups the iOS app shows, in the same order and with the same wording, so that
 * telling someone where to find something works on either platform.
 */
internal object AboutLinks {
    val discovery = listOf(
        AboutLink(
            title = R.string.settings_website_title,
            summary = R.string.about_website_summary,
            icon = R.drawable.ic_globe,
            url = R.string.settings_website_url,
        ),
        AboutLink(
            title = R.string.about_github_title,
            summary = R.string.about_github_summary,
            icon = R.drawable.ic_code,
            url = R.string.about_github_url,
        ),
    )

    val support = listOf(
        AboutLink(
            title = R.string.about_issues_title,
            summary = R.string.about_issues_summary,
            icon = R.drawable.ic_bug,
            url = R.string.about_issues_url,
        ),
        AboutLink(
            title = R.string.about_contact_title,
            summary = R.string.about_contact_summary,
            icon = R.drawable.ic_mail,
            url = R.string.about_contact_url,
        ),
    )

    val legal = listOf(
        AboutLink(
            title = R.string.about_privacy_title,
            summary = R.string.about_privacy_summary,
            icon = R.drawable.ic_shield,
            url = R.string.about_privacy_url,
        ),
    )
}

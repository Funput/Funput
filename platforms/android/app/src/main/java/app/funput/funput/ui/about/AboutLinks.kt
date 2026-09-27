package app.funput.funput.ui.about

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputTint

/** One outbound link on the about screen. URLs live in resources so they sit next to their labels. */
internal data class AboutLink(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
    @param:DrawableRes val icon: Int,
    val tint: FunputTint,
    @param:StringRes val url: Int,
)

/**
 * The same three groups the iOS app shows, in the same order and with the same wording, so that
 * telling someone where to find something works on either platform.
 */
internal object AboutLinks {
    val discovery = listOf(
        AboutLink(
            R.string.settings_website_title, R.string.about_website_summary,
            FunputIcons.Language, FunputTint.BLUE, R.string.settings_website_url,
        ),
        AboutLink(
            R.string.about_github_title, R.string.about_github_summary,
            FunputIcons.SourceCode, FunputTint.GRAY, R.string.about_github_url,
        ),
    )

    val support = listOf(
        AboutLink(
            R.string.about_issues_title, R.string.about_issues_summary,
            FunputIcons.ReportBug, FunputTint.RED, R.string.about_issues_url,
        ),
        AboutLink(
            R.string.about_contact_title, R.string.about_contact_summary,
            FunputIcons.Mail, FunputTint.GREEN, R.string.about_contact_url,
        ),
    )

    val legal = listOf(
        AboutLink(
            R.string.about_privacy_title, R.string.about_privacy_summary,
            FunputIcons.Privacy, FunputTint.PURPLE, R.string.about_privacy_url,
        ),
    )
}

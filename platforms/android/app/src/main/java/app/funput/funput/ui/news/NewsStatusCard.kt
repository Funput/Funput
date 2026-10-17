package app.funput.funput.ui.news

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Where the list stands when there is nothing better to show: loading, or a failed download,
 * worded by whether saved posts are still on screen. A failure offers to try again.
 */
@Composable
internal fun NewsStatusCard(loading: Boolean, showsSavedPosts: Boolean, onRetry: () -> Unit) {
    FunputSection(title = null, modifier = Modifier.testTag(NewsStatusTag)) {
        if (loading) {
            FunputRow(title = stringResource(R.string.news_loading))
            return@FunputSection
        }
        FunputRow(
            title = stringResource(
                if (showsSavedPosts) R.string.news_failed_stale_title else R.string.news_failed_empty_title,
            ),
            summary = stringResource(
                if (showsSavedPosts) R.string.news_failed_stale_body else R.string.news_failed_empty_body,
            ),
            icon = FunputIcons.Language,
            tint = FunputTint.GRAY,
        )
        FunputButton(
            text = stringResource(R.string.news_retry),
            onClick = onRetry,
            style = FunputButtonStyle.SECONDARY,
            modifier = Modifier.padding(FunputUi.spacing.cardPadding).testTag(NewsRetryTag),
        )
    }
}

/** Test tag of the status card. */
internal const val NewsStatusTag = "news-status"

/** Test tag of the retry button. */
internal const val NewsRetryTag = "news-retry"

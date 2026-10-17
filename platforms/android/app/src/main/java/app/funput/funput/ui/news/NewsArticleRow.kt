package app.funput.funput.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.news.model.NewsArticle
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** One post in the list: its kind and date, the title, and a two-line summary. */
@Composable
internal fun NewsArticleRow(article: NewsArticle, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val spacing = FunputUi.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.tight),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.news_open_article), role = Role.Button) {
                onOpen(article.url)
            }
            .padding(horizontal = spacing.cardPadding, vertical = spacing.medium)
            .testTag(newsArticleTag(article.id)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (article.isGuide) NewsChip(stringResource(R.string.news_chip_guide))
            article.platform?.let { NewsChip(it) }
            BasicText(ArticleDate.format(article.published), style = type.caption.copy(color = colors.secondaryLabel))
        }
        BasicText(article.title, style = type.headline.copy(color = colors.label))
        if (article.summary.isNotBlank()) {
            BasicText(
                article.summary,
                style = type.body.copy(color = colors.secondaryLabel),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NewsChip(text: String) {
    val colors = FunputUi.colors
    BasicText(
        text = text,
        style = FunputUi.typography.caption.copy(color = colors.accent),
        modifier = Modifier
            .clip(FunputUi.shapes.capsule)
            .background(colors.accent.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Posts are dated at midnight UTC; reading them in UTC keeps the day the site shows. */
private val ArticleDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.forLanguageTag("vi-VN"))
    .withZone(ZoneOffset.UTC)

/** Test tag of the row for the post [id]. */
internal fun newsArticleTag(id: String): String = "news-article-$id"

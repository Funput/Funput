package app.funput.funput.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.refresh.FunputPullToRefresh
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.news.data.NewsPhase
import app.funput.funput.ui.news.model.NewsArticle
import app.funput.funput.ui.news.model.NewsGuide

/**
 * The "Tin tức" tab: two pinned guides, then the funput.app blog newest first. Pull down to
 * download again. The list keeps the same two items in every state; loading and failures show
 * where the posts go, so nothing reshuffles while the feed arrives.
 */
@Composable
internal fun NewsScreen(
    articles: List<NewsArticle>,
    phase: NewsPhase,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    tabBar: (@Composable () -> Unit)? = null,
) {
    // The pull spinner is for a refresh over posts already shown; a first load says so in the list.
    FunputPullToRefresh(refreshing = phase == NewsPhase.LOADING && articles.isNotEmpty(), onRefresh = onRefresh) {
        FunputScreen(title = stringResource(R.string.nav_news), modifier = modifier, tabBar = tabBar) {
            item(key = "guides") { NewsGuides(onOpen) }
            item(key = "articles") { NewsArticles(articles, phase, onOpen, onRetry = onRefresh) }
        }
    }
}

@Composable
private fun NewsGuides(onOpen: (String) -> Unit) {
    FunputSection(title = stringResource(R.string.news_section_guides)) {
        NewsGuide.entries.forEachIndexed { index, guide ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            LinkRow(
                title = stringResource(guide.title),
                onClick = { onOpen(guide.url) },
                summary = stringResource(guide.summary),
                icon = if (guide == NewsGuide.SETUP) FunputIcons.Guide else FunputIcons.ToneMarks,
                external = true,
            )
        }
    }
}

@Composable
private fun NewsArticles(articles: List<NewsArticle>, phase: NewsPhase, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    if (articles.isEmpty()) {
        NewsStatusCard(loading = phase != NewsPhase.FAILED, showsSavedPosts = false, onRetry = onRetry)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.section)) {
        FunputSection(title = stringResource(R.string.news_section_articles)) {
            articles.forEachIndexed { index, article ->
                if (index > 0) FunputDivider()
                NewsArticleRow(article, onOpen)
            }
        }
        if (phase == NewsPhase.FAILED) NewsStatusCard(loading = false, showsSavedPosts = true, onRetry = onRetry)
    }
}

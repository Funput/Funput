package app.funput.funput.ui.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ui.news.data.HttpNewsFeedFetcher
import app.funput.funput.ui.news.data.NewsFeedCache
import app.funput.funput.ui.news.data.NewsModel
import java.io.File

/** The "Tin tức" tab bound to [model]: loads when shown, opens posts and guides in a Custom Tab. */
@Composable
internal fun NewsRoute(model: NewsModel, tabBar: @Composable () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(model) { model.loadIfNeeded() }
    NewsScreen(
        articles = model.articles,
        phase = model.phase,
        onRefresh = model::refresh,
        onOpen = context::openArticle,
        tabBar = tabBar,
    )
}

/**
 * The news for the whole app, made once where the navigation lives so switching tabs neither
 * forgets the posts nor downloads them again.
 */
@Composable
internal fun rememberNewsModel(): NewsModel {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        NewsModel(
            fetcher = HttpNewsFeedFetcher(),
            cache = NewsFeedCache(File(context.cacheDir, NewsFeedCache.FileName)),
            scope = scope,
        )
    }
}

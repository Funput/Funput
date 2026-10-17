package app.funput.funput.ui.news

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.funput.funput.ui.kit.layout.FunputScreenListTag
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.news.data.NewsPhase
import app.funput.funput.ui.news.model.NewsArticle
import app.funput.funput.ui.news.model.NewsFeed
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The news tab: guides on top, posts that open their page, and a way back from a failed download. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class NewsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<String>()
    private var refreshes = 0

    private fun show(articles: List<NewsArticle>, phase: NewsPhase) = compose.setContent {
        FunputUiTheme(isDark = false) {
            NewsScreen(articles, phase, onRefresh = { refreshes += 1 }, onOpen = { opened += it })
        }
    }

    @Test
    fun `guides sit above the posts, and a post opens its page`() {
        show(NewsFeed.decode(NewsFixture.Feed), NewsPhase.LOADED)

        compose.onNodeWithText("Hướng dẫn cài đặt").assertExists()
        compose.onNodeWithText("Hướng dẫn gõ tiếng Việt").assertExists()
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(newsArticleTag("new-mac")))
        compose.onNodeWithTag(newsArticleTag("new-mac")).performClick()

        compose.runOnIdle { assertEquals(listOf("https://funput.app/blog/new-mac/"), opened) }
    }

    @Test
    fun `a first download that fails asks to try again`() {
        show(emptyList(), NewsPhase.FAILED)

        compose.onNodeWithText("Chưa tải được bài viết").assertExists()
        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(NewsRetryTag))
        compose.onNodeWithTag(NewsRetryTag).performClick()

        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun `a failed refresh keeps the saved posts and says so`() {
        show(NewsFeed.decode(NewsFixture.Feed), NewsPhase.FAILED)

        compose.onNodeWithTag(FunputScreenListTag).performScrollToNode(hasTestTag(NewsStatusTag))
        compose.onNodeWithText("Chưa cập nhật được").assertExists()
    }

    @Test
    fun `a first download in progress says it is loading`() {
        show(emptyList(), NewsPhase.LOADING)

        compose.onNodeWithText("Đang tải bài viết…").assertExists()
    }
}

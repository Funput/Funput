package app.funput.funput.ui.about.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.theme.FunputUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The third-party notices, one card each, read from assets off the main thread. */
@Composable
internal fun LicensesRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val texts by produceState<Map<LicenseNotice, String?>?>(null, context) {
        value = withContext(Dispatchers.IO) { LicenseNotices.associateWith { context.readNotice(it) } }
    }
    FunputScreen(title = stringResource(R.string.licenses_title), onBack = onBack) {
        val loaded = texts
        if (loaded == null) {
            item(key = "loading") { Caption(stringResource(R.string.licenses_loading)) }
            return@FunputScreen
        }
        LicenseNotices.forEach { notice ->
            item(key = notice.assetPath) {
                FunputSection(title = stringResource(notice.title)) {
                    val text = loaded[notice]
                    Column(Modifier.padding(FunputUi.spacing.cardPadding)) {
                        if (text == null) Caption(stringResource(R.string.licenses_error)) else NoticeBody(text, notice.isMarkdown)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeBody(text: String, markdown: Boolean) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium)) {
            parseLicense(text, markdown).forEach { block ->
                when (block) {
                    is LicenseBlock.Heading -> BasicText(
                        block.text,
                        style = (if (block.level == 1) type.headline else type.label.copy(fontWeight = FontWeight.SemiBold))
                            .copy(color = colors.label),
                    )
                    is LicenseBlock.Paragraph -> BasicText(block.text, style = type.label.copy(color = colors.secondaryLabel))
                    is LicenseBlock.Quote -> BasicText(
                        block.text,
                        style = type.caption.copy(color = colors.secondaryLabel),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FunputUi.shapes.smallTile)
                            .background(colors.label.copy(alpha = QuoteFill))
                            .padding(FunputUi.spacing.medium),
                    )
                }
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    BasicText(text, style = FunputUi.typography.label.copy(color = FunputUi.colors.secondaryLabel))
}

private const val QuoteFill = 0.05f

package app.funput.funput.ui.kit.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A titled group: a small header above one [FunputCard], and an optional footer below it that
 * explains the group. Screens are a column of these.
 *
 * The header is announced as a heading, so TalkBack users can jump from group to group.
 */
@Composable
fun FunputSection(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val inset = FunputUi.spacing.cardPadding
    Column(modifier.fillMaxWidth()) {
        title?.let { FunputSectionHeader(it) }
        FunputCard(content = content)
        footer?.let {
            BasicText(
                text = it,
                style = type.caption.copy(color = colors.secondaryLabel),
                modifier = Modifier.padding(start = inset, end = inset, top = FunputUi.spacing.small),
            )
        }
    }
}

/**
 * The header on its own, for a group that is not one card: a list of cards (a theme gallery, say)
 * still reads as one titled group, named exactly like every other section.
 */
@Composable
fun FunputSectionHeader(title: String, modifier: Modifier = Modifier) {
    val inset = FunputUi.spacing.cardPadding
    BasicText(
        text = title.toUpperCase(Locale.current),
        style = FunputUi.typography.label.copy(color = FunputUi.colors.secondaryLabel),
        modifier = modifier
            .padding(start = inset, end = inset, bottom = FunputUi.spacing.small)
            .semantics { heading() },
    )
}

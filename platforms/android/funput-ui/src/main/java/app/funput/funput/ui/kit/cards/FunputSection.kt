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
        title?.let {
            BasicText(
                text = it.toUpperCase(Locale.current),
                style = type.label.copy(color = colors.secondaryLabel),
                modifier = Modifier
                    .padding(start = inset, end = inset, bottom = FunputUi.spacing.small)
                    .semantics { heading() },
            )
        }
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

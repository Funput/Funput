package app.funput.funput.ui.settings.dictionary.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.icons.FunputListIcons
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.dictionary.model.DictionaryScreenPage
import app.funput.funput.ui.settings.dictionary.model.title

/**
 * The top of the page: how many words the dictionary holds, that it stays on this device, and
 * the switch between its three lists, each named with its count.
 */
@Composable
internal fun DictionaryHeaderCard(page: DictionaryScreenPage, onSelect: (DictionaryFilter) -> Unit) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val spacing = FunputUi.spacing
    FunputCard(contentPadding = PaddingValues(spacing.cardPadding)) {
        val caption = stringResource(R.string.dictionary_total_caption)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier.clearAndSetSemantics { contentDescription = "${page.total} $caption" },
        ) {
            BasicText(page.total.toString(), style = type.display.copy(color = colors.label))
            BasicText(
                caption,
                style = type.headline.copy(color = colors.secondaryLabel),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.tight),
            modifier = Modifier.padding(top = spacing.tight),
        ) {
            Image(
                painterResource(FunputListIcons.Private),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.secondaryLabel),
                modifier = Modifier.size(16.dp),
            )
            BasicText(stringResource(R.string.dictionary_privacy), style = type.caption.copy(color = colors.secondaryLabel))
        }
        val filters = DictionaryFilter.entries
        FunputSegmented(
            options = filters.map { stringResource(R.string.dictionary_filter_option, stringResource(it.title), page.count(it)) },
            selectedIndex = filters.indexOf(page.filter),
            onSelect = { onSelect(filters[it]) },
            modifier = Modifier.padding(top = spacing.medium),
        )
    }
}

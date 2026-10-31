package app.funput.funput.ui.settings.dictionary.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ime.suggestions.dictionary.DictionarySort
import app.funput.funput.ui.kit.icons.FunputListIcons
import app.funput.funput.ui.kit.overlays.FunputMenu
import app.funput.funput.ui.kit.overlays.FunputMenuItem
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.dictionary.model.DictionaryScreenPage
import app.funput.funput.ui.settings.dictionary.model.title

/**
 * The line above the words: how many there are (or how many the search found), and the sort
 * beside it. The blocked list keeps the store's own order, so it has nothing to sort.
 */
@Composable
internal fun DictionaryCountRow(page: DictionaryScreenPage, onSort: (DictionarySort) -> Unit) {
    val total = page.count(page.filter)
    val count = if (page.isSearching) {
        stringResource(R.string.dictionary_result_count, page.words.size, total)
    } else {
        stringResource(R.string.dictionary_count, total)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = FunputUi.spacing.cardPadding),
    ) {
        BasicText(
            count,
            style = FunputUi.typography.label.copy(color = FunputUi.colors.secondaryLabel),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Spacer(Modifier.width(FunputUi.spacing.small))
        if (page.filter != DictionaryFilter.BLOCKED) DictionarySortButton(page.sort, onSort)
    }
}

/** Names the order in use and opens the choice of the three, the current one checked. */
@Composable
private fun DictionarySortButton(sort: DictionarySort, onSort: (DictionarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val accent = FunputUi.colors.accent
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .clip(FunputUi.shapes.capsule)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.dictionary_sort)) { expanded = true }
                .padding(horizontal = FunputUi.spacing.small),
        ) {
            BasicText(stringResource(sort.title), style = FunputUi.typography.label.copy(color = accent))
            Image(painterResource(FunputListIcons.Sort), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(accent))
        }
        FunputMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            items = DictionarySort.entries.map { option ->
                FunputMenuItem(stringResource(option.title), { onSort(option) }, selected = option == sort)
            },
        )
    }
}

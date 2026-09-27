package app.funput.funput.ui.shortcuts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.theme.FunputUi

/** While the library loads, says so; if it failed, says that and offers to try again. */
@Composable
internal fun ShortcutsLoadStatus(loading: Boolean, error: Boolean, retry: () -> Unit) {
    if (!loading && !error) return
    StatusCard(
        title = stringResource(if (loading) R.string.shortcuts_loading else R.string.shortcuts_load_error),
        body = null,
        actionLabel = if (error) stringResource(R.string.shortcuts_retry) else null,
        action = retry,
    )
}

/**
 * Nothing to list: [title] and [body] say why (no shortcuts yet, or none matching the search),
 * and [action], when given, offers to add the first one.
 */
@Composable
internal fun ShortcutsEmptyState(title: String, body: String, action: (() -> Unit)? = null) {
    StatusCard(title, body, action?.let { stringResource(R.string.shortcuts_add) }, action ?: {})
}

@Composable
private fun StatusCard(title: String, body: String?, actionLabel: String?, action: () -> Unit) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    FunputCard(contentPadding = PaddingValues(FunputUi.spacing.section)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.small),
        ) {
            BasicText(title, style = type.headline.copy(color = colors.label, textAlign = TextAlign.Center))
            body?.let {
                BasicText(it, style = type.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center))
            }
            actionLabel?.let { FunputButton(it, onClick = action, style = FunputButtonStyle.SECONDARY) }
        }
    }
}

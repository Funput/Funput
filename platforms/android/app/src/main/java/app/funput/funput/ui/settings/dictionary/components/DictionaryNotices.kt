package app.funput.funput.ui.settings.dictionary.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * What stands between the user and a dictionary that learns: the suggestion switch, or a wipe
 * the keyboard has not carried out yet. Each notice says why and, where it can, offers the one
 * thing that fixes it.
 */
@Composable
internal fun DictionaryNotices(suggestionsEnabled: Boolean, resetPending: Boolean, onEnableSuggestions: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium)) {
        if (!suggestionsEnabled) {
            Notice(
                message = stringResource(R.string.dictionary_notice_off),
                icon = FunputIcons.Suggestions,
                tint = FunputTint.PURPLE,
                actionLabel = stringResource(R.string.dictionary_notice_turn_on),
                onAction = onEnableSuggestions,
            )
        }
        if (resetPending) {
            Notice(
                message = stringResource(R.string.dictionary_notice_reset_pending),
                icon = FunputIcons.ClipboardExpiry,
                tint = FunputTint.GRAY,
            )
        }
    }
}

@Composable
private fun Notice(
    message: String,
    @DrawableRes icon: Int,
    tint: FunputTint,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val spacing = FunputUi.spacing
    FunputCard {
        FunputRow(title = message, icon = icon, tint = tint)
        actionLabel?.let {
            FunputButton(
                text = it,
                onClick = onAction,
                style = FunputButtonStyle.SECONDARY,
                modifier = Modifier.padding(start = spacing.cardPadding, end = spacing.cardPadding, bottom = spacing.medium),
            )
        }
    }
}

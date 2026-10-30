package app.funput.funput.ui.settings.enterkey.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.theme.FunputUi

/** No app chosen yet: what the page is for, and a way to choose the first app. */
@Composable
internal fun EnterKeyEmptyState(onAdd: () -> Unit) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    FunputCard(contentPadding = PaddingValues(FunputUi.spacing.section)) {
        // Full width, or the card's start-aligned column parks the centred block on the left.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.small),
        ) {
            BasicText(
                stringResource(R.string.settings_enter_key_empty_title),
                style = type.headline.copy(color = colors.label, textAlign = TextAlign.Center),
            )
            BasicText(
                stringResource(R.string.settings_enter_key_empty_body),
                style = type.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
            )
            FunputButton(
                text = stringResource(R.string.settings_enter_key_add),
                onClick = onAdd,
                style = FunputButtonStyle.SECONDARY,
            )
        }
    }
}

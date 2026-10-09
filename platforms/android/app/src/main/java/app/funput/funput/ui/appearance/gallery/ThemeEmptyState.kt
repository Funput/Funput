package app.funput.funput.ui.appearance.gallery

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

/**
 * What "your themes" shows before there is one: where a new theme will land, and a way to make
 * it, so the section answers "have I made any yet?" instead of vanishing.
 */
@Composable
internal fun ThemeEmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    FunputCard(modifier = modifier, contentPadding = PaddingValues(FunputUi.spacing.section)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.small),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BasicText(
                text = stringResource(R.string.theme_gallery_empty_title),
                style = type.headline.copy(color = colors.label, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(R.string.theme_gallery_empty_body),
                style = type.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center),
            )
            FunputButton(
                text = stringResource(R.string.theme_gallery_create_title),
                onClick = onCreate,
                style = FunputButtonStyle.SECONDARY,
            )
        }
    }
}

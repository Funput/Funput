package app.funput.funput.ui.kit.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A centred message where a list would be: loading, a failure, or nothing yet. [title] says what
 * is going on, [body] (optional) why, and [actionLabel] with [onAction] offers the way forward.
 */
@Composable
fun FunputMessageCard(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    FunputCard(modifier = modifier, contentPadding = PaddingValues(FunputUi.spacing.section)) {
        // Full width, or the card's start-aligned column parks the centred block on the left.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.small),
        ) {
            BasicText(title, style = type.headline.copy(color = colors.label, textAlign = TextAlign.Center))
            body?.let {
                BasicText(it, style = type.body.copy(color = colors.secondaryLabel, textAlign = TextAlign.Center))
            }
            actionLabel?.let { FunputButton(it, onClick = onAction, style = FunputButtonStyle.SECONDARY) }
        }
    }
}

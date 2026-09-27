package app.funput.funput.ui.theme.custom.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * The picture's own colours, offered as accents: a background on a palette that knows nothing
 * about it is what makes a theme look assembled rather than designed. Offered, not applied, since
 * which of a photo's colours should lead is a judgement, not a computation.
 */
@Composable
internal fun ImagePaletteSection(colours: List<Int>, onSelected: (Int) -> Unit) {
    if (colours.isEmpty()) return
    val description = stringResource(R.string.custom_theme_palette_apply)
    FunputSection(
        title = stringResource(R.string.custom_theme_palette_title),
        footer = stringResource(R.string.custom_theme_palette_hint),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(FunputUi.spacing.cardPadding),
        ) {
            colours.forEach { colour ->
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(FunputUi.shapes.capsule)
                        .background(Color(colour))
                        .border(1.dp, FunputUi.colors.separator, FunputUi.shapes.capsule)
                        .clickable(role = Role.Button) { onSelected(colour) }
                        .semantics { contentDescription = description },
                )
            }
        }
    }
}

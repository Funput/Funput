package app.funput.funput.ui.appearance.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.controls.FunputIconButton
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.navigation.sharedElementByKey
import app.funput.funput.ui.theme.KeyboardThemePreview
import app.funput.funput.ui.theme.localizedName
import app.funput.funput.ui.theme.themePreviewSharedKey

/**
 * One theme, at the width of the keyboard it previews: a keyboard fills the screen edge to edge,
 * so a narrower preview would squash it and stop looking like the thing being chosen.
 *
 * The theme in use carries an accent outline and an "in use" badge. [onMore], given for themes the
 * user made, adds a button that opens their edit/delete actions, kept off the card's face so a
 * destructive action is never one stray tap away from a card whose job is to be tapped.
 */
@Composable
internal fun ThemeCard(
    descriptor: KeyboardThemeDescriptor,
    selected: Boolean,
    onSelected: () -> Unit,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = FunputUi.colors
    val shape = FunputUi.shapes.card
    val title = descriptor.localizedName()
    FunputCard(
        modifier = modifier.then(if (selected) Modifier.border(2.dp, colors.accent, shape) else Modifier),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
            modifier = Modifier
                .testTag(descriptor.id.value)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelected)
                .padding(FunputUi.spacing.medium),
        ) {
            KeyboardThemePreview(
                theme = descriptor.theme,
                backgroundImage = descriptor.backgroundImage,
                modifier = Modifier
                    .sharedElementByKey(themePreviewSharedKey(descriptor.id))
                    .fillMaxWidth()
                    .aspectRatio(PreviewAspect)
                    .clip(FunputUi.shapes.thumbnail),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = FunputUi.spacing.tight)) {
                    BasicText(title, style = FunputUi.typography.headline.copy(color = colors.label))
                    BasicText(
                        text = stringResource(R.string.theme_gallery_author, descriptor.author),
                        style = FunputUi.typography.caption.copy(color = colors.secondaryLabel),
                    )
                }
                if (selected) InUseBadge()
                onMore?.let { more ->
                    FunputIconButton(
                        icon = FunputIcons.More,
                        contentDescription = stringResource(R.string.theme_gallery_more_description, title),
                        onClick = more,
                    )
                }
            }
        }
    }
}

@Composable
private fun InUseBadge() {
    val colors = FunputUi.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight),
        modifier = Modifier
            .clip(FunputUi.shapes.capsule)
            .background(colors.accent.copy(alpha = BadgeFillAlpha))
            .padding(BadgePadding),
    ) {
        Image(
            painter = painterResource(FunputIcons.Check),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.accent),
            modifier = Modifier.size(14.dp),
        )
        BasicText(
            text = stringResource(R.string.theme_gallery_selected),
            style = FunputUi.typography.caption.copy(color = colors.accent),
        )
    }
}

/** Roughly the shape of the real keyboard: full width, a little under half as tall. */
private const val PreviewAspect = 2.05f

private const val BadgeFillAlpha = 0.14f

private val BadgePadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)

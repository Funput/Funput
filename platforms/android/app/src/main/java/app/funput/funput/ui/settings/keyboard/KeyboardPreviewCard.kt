package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.theme.KeyboardThemePreview
import app.funput.funput.ui.theme.KeyboardThemePreviewConfiguration
import app.funput.funput.ui.theme.localizedName

/** Width of the keyboard thumbnail: a third of a phone's card, enough to recognise the theme. */
private val PreviewWidth = 132.dp

/** The width the keyboard is drawn at before being scaled down: a typical phone's. */
private val RenderWidth = 400.dp

/** Roughly the keyboard's own shape, without and with the number row. */
private const val PreviewAspect = 2.05f
private const val NumberRowAspect = 1.72f

/**
 * The keyboard the user types on, at the top of its settings: a small live preview in the chosen
 * theme next to the theme's name. Tapping it opens the Appearance tab. Deliberately compact, so it
 * identifies the keyboard without pushing every setting below the fold.
 */
@Composable
internal fun KeyboardPreviewCard(
    descriptor: KeyboardThemeDescriptor,
    inputMethod: KeyboardInputMethod,
    sizingProfile: KeyboardSizingProfile,
    showsNumberRow: Boolean,
    onOpenAppearance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // VNI types tones with the digits, so its keyboard always has the number row.
    val numberRow = showsNumberRow || !inputMethod.isTelexFamily
    val colors = FunputUi.colors
    FunputCard(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpenAppearance)
                .padding(FunputUi.spacing.medium),
        ) {
            val aspect = if (numberRow) NumberRowAspect else PreviewAspect
            Box(
                Modifier
                    .width(PreviewWidth)
                    .height(PreviewWidth / aspect)
                    .clip(FunputUi.shapes.thumbnail)
                    .background(Color(descriptor.theme.backgroundEndColor)),
            ) {
                // The renderer sizes keys and labels in real dp, so drawing it 132dp wide would
                // cram full-size labels into tiny keys. Draw it at a phone's width and scale the
                // whole picture down instead, which keeps it a faithful miniature.
                KeyboardThemePreview(
                    theme = descriptor.theme,
                    backgroundImage = descriptor.backgroundImage,
                    configuration = KeyboardThemePreviewConfiguration(
                        inputMethod = inputMethod,
                        sizingProfile = sizingProfile,
                        showsNumberRow = numberRow,
                    ),
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(RenderWidth, RenderWidth / aspect)
                        .graphicsLayer {
                            val scale = PreviewWidth / RenderWidth
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = FunputUi.spacing.medium)) {
                BasicText(descriptor.localizedName(), style = FunputUi.typography.headline.copy(color = colors.label))
                BasicText(
                    text = stringResource(R.string.settings_keyboard_hero_hint),
                    style = FunputUi.typography.caption.copy(color = colors.secondaryLabel),
                )
            }
            Image(
                painter = painterResource(FunputIcons.Forward),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.tertiaryLabel),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

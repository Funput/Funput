package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.theme.tint
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

/**
 * The two steps between installing Funput and typing with it (enable it, then select it), shown
 * until both are done and then not at all: a banner that stays after setup only congratulates the
 * user, forever, for something they did once.
 */
@Composable
internal fun KeyboardSetupCard(
    status: KeyboardSetupStatus,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status == KeyboardSetupStatus.READY) return
    val enabling = status == KeyboardSetupStatus.NOT_ENABLED
    val colors = FunputUi.colors
    val spacing = FunputUi.spacing
    FunputCard(modifier, contentPadding = PaddingValues(spacing.cardPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandTile()
            Spacer(Modifier.width(spacing.medium))
            Column {
                BasicText(
                    text = stringResource(R.string.settings_keyboard_setup_heading),
                    style = FunputUi.typography.headline.copy(color = colors.label),
                )
                BasicText(
                    text = stringResource(R.string.settings_keyboard_setup_progress, if (enabling) 1 else 2),
                    style = FunputUi.typography.caption.copy(color = colors.secondaryLabel),
                )
            }
        }
        Spacer(Modifier.height(spacing.large))
        SetupStep(
            index = 1,
            state = if (enabling) StepState.ACTIVE else StepState.DONE,
            title = stringResource(R.string.settings_keyboard_setup_step_enable),
            connected = true,
        )
        SetupStep(
            index = 2,
            state = if (enabling) StepState.UPCOMING else StepState.ACTIVE,
            title = stringResource(R.string.settings_keyboard_setup_step_select),
            connected = false,
        )
        Spacer(Modifier.height(spacing.large))
        FunputButton(
            text = stringResource(
                if (enabling) R.string.settings_keyboard_setup_enable_action
                else R.string.settings_keyboard_setup_select_action,
            ),
            onClick = if (enabling) onEnableKeyboard else onSelectKeyboard,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The card's badge: the one brand gradient on the page, so it reads as identity, not decoration. */
@Composable
private fun BrandTile() {
    val colors = FunputUi.colors
    val sweep = listOf(FunputTint.ORANGE, FunputTint.PINK, FunputTint.PURPLE).map { colors.tint(it) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp).clip(FunputUi.shapes.iconTile).background(Brush.linearGradient(sweep)),
    ) {
        Image(
            painter = painterResource(FunputIcons.Keyboard),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(24.dp),
        )
    }
}

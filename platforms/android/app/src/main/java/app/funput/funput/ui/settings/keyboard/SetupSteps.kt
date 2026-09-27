package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUi

/** Where a setup step stands. */
internal enum class StepState { DONE, ACTIVE, UPCOMING }

private val BadgeSize = 28.dp

/**
 * One numbered step of keyboard setup: a badge (a check once done, the number otherwise), the
 * step's text, and, when [connected], a line down to the next step that fills in once done.
 */
@Composable
internal fun SetupStep(index: Int, state: StepState, title: String, connected: Boolean) {
    val colors = FunputUi.colors
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(BadgeSize)) {
            StepBadge(index, state)
            if (connected) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(if (state == StepState.DONE) colors.accent else colors.separator),
                )
            }
        }
        Spacer(Modifier.width(FunputUi.spacing.medium))
        BasicText(
            text = title,
            style = FunputUi.typography.body.copy(
                color = if (state == StepState.UPCOMING) colors.secondaryLabel else colors.label,
                fontWeight = if (state == StepState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
            ),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StepBadge(index: Int, state: StepState) {
    val colors = FunputUi.colors
    val capsule = FunputUi.shapes.capsule
    val filled = state != StepState.UPCOMING
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BadgeSize)
            .clip(capsule)
            .then(
                if (filled) Modifier.background(colors.accent)
                else Modifier.border(1.5.dp, colors.separator, capsule),
            ),
    ) {
        if (state == StepState.DONE) {
            Image(
                painter = painterResource(FunputIcons.Check),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onAccent),
                modifier = Modifier.size(16.dp),
            )
        } else {
            BasicText(
                text = index.toString(),
                style = FunputUi.typography.label.copy(
                    color = if (filled) colors.onAccent else colors.secondaryLabel,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

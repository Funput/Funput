package app.funput.funput.ui.kit.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.DisabledAlpha
import app.funput.funput.ui.kit.theme.FunputColors
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tokens.OpacityTokens

/** How loud a [FunputButton] is. */
enum class FunputButtonStyle {
    /** The one main action of a screen or card: filled with the accent. */
    PRIMARY,

    /** A supporting action: an accent-tinted fill. */
    SECONDARY,

    /** A quiet action, text only: dialog buttons, "Skip". */
    PLAIN,

    /** An action that deletes or resets something: tinted with the destructive colour. */
    DESTRUCTIVE,
}

/**
 * A capsule button at least 48dp tall. Pass `Modifier.fillMaxWidth()` for a full-width call to
 * action; by default it wraps its label.
 */
@Composable
fun FunputButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FunputButtonStyle = FunputButtonStyle.PRIMARY,
    enabled: Boolean = true,
) {
    val colors = FunputUi.colors
    val (fill, content) = style.colors(colors)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp, minWidth = 64.dp)
            .clip(FunputUi.shapes.capsule)
            .background(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .padding(horizontal = FunputUi.spacing.large, vertical = FunputUi.spacing.medium),
    ) {
        BasicText(
            text = text,
            style = FunputUi.typography.label.copy(
                color = content,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private fun FunputButtonStyle.colors(colors: FunputColors): Pair<Color, Color> = when (this) {
    FunputButtonStyle.PRIMARY -> colors.accent to colors.onAccent
    FunputButtonStyle.SECONDARY -> colors.accent.copy(alpha = OpacityTokens.tintFill) to colors.accent
    FunputButtonStyle.PLAIN -> Color.Transparent to colors.accent
    FunputButtonStyle.DESTRUCTIVE -> colors.destructive.copy(alpha = OpacityTokens.tintFill) to colors.destructive
}

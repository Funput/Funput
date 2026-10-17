package app.funput.funput.ui.kit.tiles

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A quick-settings tile: an icon, a short [title] and a [state] line, the whole tile one switch.
 * On, it takes a wash of the accent and its icon and state turn accent; off, it is a plain card.
 * A [locked] tile shows its value but cannot be changed, and says why through [lockedReason].
 * With [onClick], tapping opens a choice (a sheet of strengths, say) instead of flipping the tile,
 * and the tile reads as a button that names it through [onClickLabel].
 *
 * TalkBack reads [accessibilityTitle], the longer name, with the state, as a switch.
 */
@Composable
fun FunputToggleTile(
    title: String,
    state: String,
    @DrawableRes icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accessibilityTitle: String = title,
    locked: Boolean = false,
    lockedReason: String? = null,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val shape = FunputUi.shapes.interactiveCard
    val tint = if (checked) colors.accent else colors.secondaryLabel
    Column(
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight),
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(shape)
            .background(colors.cardBackground)
            .then(if (checked) Modifier.background(colors.accent.copy(alpha = 0.14f)) else Modifier)
            .border(FunputUi.spacing.cardStrokeWidth, colors.cardStroke, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = !locked, onClickLabel = onClickLabel, role = Role.Button) {
                        onClick()
                    }
                } else {
                    Modifier.toggleable(checked, enabled = !locked, role = Role.Switch, onValueChange = onCheckedChange)
                },
            )
            .semantics {
                contentDescription = accessibilityTitle
                stateDescription = listOfNotNull(state, lockedReason?.takeIf { locked }).joinToString(". ")
            }
            .alpha(if (locked) 0.7f else 1f)
            .padding(FunputUi.spacing.medium),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(24.dp),
        )
        // The tile speaks for itself through its own description; the texts would only repeat it.
        BasicText(
            text = title,
            style = type.body.copy(color = colors.label),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {},
        )
        BasicText(
            text = state,
            style = type.caption.copy(color = tint),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

package app.funput.funput.ui.kit.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tokens.OpacityTokens

/**
 * A choice between a few short options shown side by side, the selected one raised on a card-
 * coloured capsule. For more than three or four options, or long labels, use a picker sheet.
 *
 * The control is 48dp tall; each segment is 42dp inside a 3dp inset, and TalkBack reads the
 * segments as radio buttons in one group.
 */
@Composable
fun FunputSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FunputUi.colors
    val capsule = FunputUi.shapes.capsule
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(capsule)
            .background(colors.label.copy(alpha = OpacityTokens.tintFill / 2))
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val fill by animateColorAsState(
                if (selected) colors.cardBackground else Color.Transparent,
                FunputMotion.selection(),
                label = "segment-fill",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(capsule)
                    .background(fill)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .padding(horizontal = FunputUi.spacing.small),
            ) {
                BasicText(
                    text = option,
                    style = FunputUi.typography.label.copy(
                        color = if (selected) colors.label else colors.secondaryLabel,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

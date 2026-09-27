package app.funput.funput.ui.kit.layout

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tokens.OpacityTokens

/**
 * One tab of a [FunputTabBar]: its icon over its label, both in the accent on a tinted capsule
 * while [selected]. With few tabs every one is named; the selection shows by colour, not by
 * hiding the others' names.
 */
@Composable
internal fun TabItem(tab: FunputTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FunputUi.colors
    val fill by animateColorAsState(
        if (selected) colors.accent.copy(alpha = OpacityTokens.tintFill) else Color.Transparent,
        FunputMotion.selection(),
        label = "tab-fill",
    )
    val tint by animateColorAsState(
        if (selected) colors.accent else colors.secondaryLabel,
        FunputMotion.selection(),
        label = "tab-tint",
    )
    val icon = if (selected) tab.selectedIcon ?: tab.icon else tab.icon
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxHeight()
            .clip(FunputUi.shapes.capsule)
            .background(fill)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            // Announced once through the description above, not as an icon plus a separate label.
            .clearAndSetSemantics { contentDescription = tab.label }
            .padding(horizontal = 8.dp),
    ) {
        icon?.let {
            Image(
                painter = painterResource(it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(20.dp),
            )
        }
        BasicText(
            text = tab.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = FunputUi.typography.caption.copy(color = tint, fontWeight = FontWeight.SemiBold),
        )
    }
}

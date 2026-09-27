package app.funput.funput.ui.kit.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tokens.OpacityTokens

/** One tab of a [FunputTabBar]: its icon, and its label beside it while [selected]. */
@Composable
internal fun TabItem(tab: FunputTab, selected: Boolean, onClick: () -> Unit) {
    val colors = FunputUi.colors
    val capsule = FunputUi.shapes.capsule
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(TabHeight)
            .widthIn(min = MinTabWidth)
            .clip(capsule)
            .background(fill)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = tab.label }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        // What is drawn is announced through the tab's own description, once and the same for
        // every tab, rather than as an icon and, for one tab only, a visible label.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics {}) {
            icon?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(tint),
                    modifier = Modifier.size(22.dp),
                )
            }
            AnimatedVisibility(
                visible = selected || icon == null,
                enter = fadeIn(FunputMotion.selection()) + expandHorizontally(FunputMotion.selection()),
                exit = fadeOut(FunputMotion.selection()) + shrinkHorizontally(FunputMotion.selection()),
            ) {
                BasicText(
                    text = tab.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = FunputUi.typography.label.copy(color = tint, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp),
                )
            }
        }
    }
}

/** An icon-only tab is still a comfortable touch target. */
private val MinTabWidth = 56.dp

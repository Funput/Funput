package app.funput.funput.ui.kit.layout

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.glass.LocalGlassBackdrop
import app.funput.funput.ui.kit.glass.funputGlass
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tokens.OpacityTokens

/** Space a [FunputScreen] keeps free at the bottom so the last item clears the tab bar. */
internal val TabBarReserve = 64.dp + 12.dp * 2

/** One destination in a [FunputTabBar]. */
@Immutable
data class FunputTab(
    /** Short label under the icon. */
    val label: String,
    /** Optional icon, tinted by the bar. */
    @param:DrawableRes val icon: Int? = null,
    /** Icon while selected, usually the filled form of [icon]; falls back to [icon]. */
    @param:DrawableRes val selectedIcon: Int? = null,
)

/**
 * The floating pill tab bar, drawn as glass over the screen's content. The selected tab carries
 * an accent-tinted capsule; tabs are announced as tabs in one selectable group.
 */
@Composable
fun FunputTabBar(
    tabs: List<FunputTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FunputUi.colors
    val capsule = FunputUi.shapes.capsule
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .height(64.dp)
            .funputGlass(LocalGlassBackdrop.current, capsule, colors)
            .selectableGroup()
            .padding(6.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val fill by animateColorAsState(
                if (selected) colors.accent.copy(alpha = OpacityTokens.tintFill) else Color.Transparent,
                FunputMotion.selection(),
                label = "tab-fill",
            )
            val tint = if (selected) colors.accent else colors.secondaryLabel
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(capsule)
                    .background(fill)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
            ) {
                (if (selected) tab.selectedIcon ?: tab.icon else tab.icon)?.let {
                    Image(
                        painter = painterResource(it),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(tint),
                        modifier = Modifier.size(22.dp),
                    )
                }
                BasicText(
                    text = tab.label,
                    style = FunputUi.typography.caption.copy(color = tint, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

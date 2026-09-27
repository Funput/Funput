package app.funput.funput.ui.kit.layout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.glass.LocalGlassBackdrop
import app.funput.funput.ui.kit.glass.funputGlass
import app.funput.funput.ui.kit.theme.FunputUi

/** Height of the bar's pill: an icon over a caption, with room to grow at large font sizes. */
private val BarHeight = 56.dp

/** Gap between the pill and the navigation bar (or the screen's bottom edge). */
private val BarBottomMargin = 8.dp

/** Space a [FunputScreen] keeps free at the bottom so the last item clears the tab bar. */
internal val TabBarReserve = BarHeight + BarBottomMargin + 16.dp

/** One destination in a [FunputTabBar]. */
@Immutable
data class FunputTab(
    /** Name of the destination, shown under its icon and read aloud. */
    val label: String,
    /** Optional icon, tinted by the bar. */
    @param:DrawableRes val icon: Int? = null,
    /** Icon while selected, usually the filled form of [icon]; falls back to [icon]. */
    @param:DrawableRes val selectedIcon: Int? = null,
)

/**
 * The floating tab bar: a frosted glass pill above the navigation bar, its tabs sharing the width
 * equally, each an icon over its name. The selected tab takes the accent on a tinted capsule. Tabs
 * are announced by name, as tabs in one selectable group.
 */
@Composable
fun FunputTabBar(
    tabs: List<FunputTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 24.dp, end = 24.dp, bottom = BarBottomMargin),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                // As tall as the tallest tab, never shorter than the bar: tabs fill that height.
                .heightIn(min = BarHeight)
                .height(IntrinsicSize.Min)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .funputGlass(LocalGlassBackdrop.current, FunputUi.shapes.capsule, FunputUi.colors, frosted = true)
                .selectableGroup()
                .padding(4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                TabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

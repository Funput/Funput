package app.funput.funput.ui.kit.layout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.glass.LocalGlassBackdrop
import app.funput.funput.ui.kit.glass.funputGlass
import app.funput.funput.ui.kit.theme.FunputUi

/** Height of the bar's pill. */
private val BarHeight = 52.dp

/** Height of a tab inside the pill, which pads it by 4dp all round. */
internal val TabHeight = BarHeight - 8.dp

/** Gap between the pill and the navigation bar (or the screen's bottom edge). */
private val BarBottomMargin = 8.dp

/** Space a [FunputScreen] keeps free at the bottom so the last item clears the tab bar. */
internal val TabBarReserve = BarHeight + BarBottomMargin + 16.dp

/** One destination in a [FunputTabBar]. */
@Immutable
data class FunputTab(
    /** Name of the destination: shown beside the icon while selected, and always read aloud. */
    val label: String,
    /** Optional icon, tinted by the bar; without one the label is always shown. */
    @param:DrawableRes val icon: Int? = null,
    /** Icon while selected, usually the filled form of [icon]; falls back to [icon]. */
    @param:DrawableRes val selectedIcon: Int? = null,
)

/**
 * The floating tab bar: a glass pill as wide as its tabs, centred above the navigation bar. Each
 * tab is an icon; the selected one grows into an accent capsule that names it beside its icon, so
 * the bar stays compact yet always says where you are. Every tab is announced by its label, as a
 * tab in one selectable group.
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
                .height(BarHeight)
                .widthIn(max = 480.dp)
                .funputGlass(LocalGlassBackdrop.current, FunputUi.shapes.capsule, FunputUi.colors, frosted = true)
                .selectableGroup()
                .padding(4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                TabItem(tab, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

package app.funput.funput.ui.kit.layout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.R
import app.funput.funput.ui.kit.glass.LocalGlassBackdrop
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.glass.funputGlass
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputUi
import com.kyant.shapes.RoundedRectangle

/** Height of the bar below the status bar. */
internal val TopBarHeight = 52.dp

/** Glass is drawn edge to edge, so the bar's shape has square corners. */
private val BarShape = RoundedRectangle(0.dp)

/**
 * The bar over a [FunputScreen]: invisible while the large title is in view, then glass with a
 * compact title once content scrolls under it. The back button and the actions, when present,
 * are always shown.
 */
@Composable
internal fun FunputTopBar(
    title: String,
    collapsed: Boolean,
    onBack: (() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = FunputUi.colors
    val chrome by animateFloatAsState(if (collapsed) 1f else 0f, FunputMotion.selection(), label = "top-bar")
    val backdrop = LocalGlassBackdrop.current
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = chrome }
                .funputGlass(backdrop, BarShape, colors, refract = false),
        )
        CenteredBar(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(TopBarHeight),
            start = { onBack?.let { BackButton(it, Modifier) } },
            title = {
                BasicText(
                    text = title,
                    style = FunputUi.typography.headline.copy(color = colors.label),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp).graphicsLayer { alpha = chrome },
                )
            },
            end = {
                actions?.let { Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically, content = it) }
            },
        )
    }
}

/**
 * Back button at the start, actions at the end, and the title centred in what they leave. The
 * room kept on each side is the wider of the two, so the title stays centred on the screen and
 * never runs under a wide action such as a text button at a large font.
 */
@Composable
private fun CenteredBar(
    modifier: Modifier,
    start: @Composable () -> Unit,
    title: @Composable () -> Unit,
    end: @Composable () -> Unit,
) {
    Layout(contents = listOf(start, title, end), modifier = modifier) { (starts, titles, ends), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val startPlaceables = starts.map { it.measure(loose) }
        val endPlaceables = ends.map { it.measure(loose) }
        val side = maxOf(
            startPlaceables.maxOfOrNull { it.width } ?: 0,
            endPlaceables.maxOfOrNull { it.width } ?: 0,
            MinSide.roundToPx(),
        )
        val titleWidth = (constraints.maxWidth - 2 * side).coerceAtLeast(0)
        val titlePlaceables = titles.map { it.measure(loose.copy(maxWidth = titleWidth)) }
        val height = constraints.maxHeight
        layout(constraints.maxWidth, height) {
            startPlaceables.forEach { it.place(0, (height - it.height) / 2) }
            endPlaceables.forEach { it.place(constraints.maxWidth - it.width, (height - it.height) / 2) }
            titlePlaceables.forEach { it.place((constraints.maxWidth - it.width) / 2, (height - it.height) / 2) }
        }
    }
}

/** Room kept on each side of the title even with nothing there, so it never touches the edge. */
private val MinSide = 16.dp

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(start = 4.dp)
            .size(48.dp)
            .clip(FunputUi.shapes.capsule)
            .clickable(
                onClickLabel = stringResource(R.string.funput_ui_back),
                role = Role.Button,
                onClick = onBack,
            ),
    ) {
        Image(
            painter = painterResource(FunputIcons.Back),
            contentDescription = stringResource(R.string.funput_ui_back),
            colorFilter = ColorFilter.tint(FunputUi.colors.accent),
            modifier = Modifier.size(24.dp),
        )
    }
}

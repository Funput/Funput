package app.funput.funput.ui.kit.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.glass.LocalGlassBackdrop
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A screen for editing one thing, where what is being edited must stay in view: a fixed top bar
 * with its title, a pinned [header] (a live preview, say) that never scrolls away, [content] filling
 * the middle and scrolling on its own, and an optional [bottomBar] of actions, always reachable.
 *
 * Unlike [FunputScreen] there is no large title: the header is the screen's subject, and the room a
 * large title takes is room the content needs. The whole screen rises above the keyboard.
 *
 * The bars sit beside the content rather than over it, so they are solid: there is nothing behind
 * them to see through. No backdrop is provided, which also keeps any glass inside [header] or
 * [content] from sampling a layer that contains itself.
 */
@Composable
fun FunputEditorScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = FunputUi.colors
    CompositionLocalProvider(LocalGlassBackdrop provides null) {
        Column(
            modifier
                .fillMaxSize()
                .background(colors.groupedBackground)
                .imePadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            FunputTopBar(title = title, collapsed = true, onBack = onBack, actions = actions)
            header?.let { Column(Modifier.fillMaxWidth(), content = it) }
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            bottomBar?.let { bar ->
                Column(Modifier.fillMaxWidth().background(colors.cardBackground)) {
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
                    Box(Modifier.navigationBarsPadding()) { bar() }
                }
            }
        }
    }
}

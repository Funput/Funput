package app.funput.funput.keyboard.ui.panel

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import app.funput.funput.keyboard.ui.panel.indication.PanelPressIndication

internal open class KeyboardPanelComposeView(context: Context) : FrameLayout(context) {
    private val composeView = ComposeView(context)

    /** Colours of the theme in use; null until the first theme arrives. */
    protected var palette by mutableStateOf<KeyboardPanelPalette?>(null)

    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Hosts [content] with press feedback in the theme's colour instead of Compose's default. */
    fun setContent(content: @Composable () -> Unit) = composeView.setContent {
        val pressed = palette?.pressed
        val indication = remember(pressed) { PanelPressIndication(pressed) }
        CompositionLocalProvider(LocalIndication provides indication, content = content)
    }
}

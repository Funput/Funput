package app.funput.funput.keyboard.ui.panel

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

internal open class KeyboardPanelComposeView(context: Context) : FrameLayout(context) {
    private val composeView = ComposeView(context)

    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setContent(content: @Composable () -> Unit) = composeView.setContent(content)
}

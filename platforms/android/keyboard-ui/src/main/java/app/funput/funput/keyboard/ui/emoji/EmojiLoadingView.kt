package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import app.funput.funput.keyboard.ui.R

internal class EmojiLoadingView(context: Context) : LinearLayout(context) {
    private val progress = ProgressBar(context)
    private val label = TextView(context).apply {
        setText(R.string.emoji_panel_loading)
        gravity = Gravity.CENTER
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        addView(progress, LayoutParams(dp(28), dp(28)))
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })
    }

    fun showEmpty() {
        progress.visibility = GONE
        label.setText(R.string.emoji_panel_empty)
    }

    fun updatePalette(palette: EmojiPanelPalette) {
        progress.indeterminateTintList = ColorStateList.valueOf(palette.accent)
        label.setTextColor(palette.secondaryLabel)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

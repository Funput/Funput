package app.funput.funput.keyboard.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import app.funput.funput.keyboard.ui.R
import app.funput.funput.theme.KeyboardTheme

internal class EmojiPanelToolbar(context: Context) : LinearLayout(context) {
    var onLettersRequested: () -> Unit = {}
    var onBackspaceRequested: () -> Unit = {}

    private val lettersButton = actionButton(
        text = context.getString(R.string.emoji_panel_letters),
        description = context.getString(R.string.emoji_panel_letters_description),
    ) { onLettersRequested() }
    private val backspaceButton = actionButton(
        text = "⌫",
        description = context.getString(R.string.emoji_panel_backspace_description),
    ) { onBackspaceRequested() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val inset = dp(8)
        setPadding(inset, dp(4), inset, dp(6))
        addView(lettersButton, buttonLayoutParams())
        addView(View(context), LayoutParams(0, 0, 1f))
        addView(backspaceButton, buttonLayoutParams())
    }

    fun updateTheme(theme: KeyboardTheme) {
        listOf(lettersButton, backspaceButton).forEach { button ->
            button.setTextColor(theme.labelColor)
            button.background = EmojiPanelBackgrounds.button(theme, resources.displayMetrics.density)
        }
    }

    private fun actionButton(text: String, description: String, action: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            contentDescription = description
            gravity = Gravity.CENTER
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun buttonLayoutParams() = LayoutParams(dp(64), LayoutParams.MATCH_PARENT)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

package app.funput.funput.keyboard.ui.placement

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Modal mode chooser shown by the keyboard toolbar placement action. */
internal class KeyboardPlacementModePickerView(context: Context) : FrameLayout(context) {
    private val density = resources.displayMetrics.density
    private val title = TextView(context).apply {
        text = resources.getString(R.string.placement_choose_mode)
        textSize = 20f
        setPadding(dp(18), dp(14), dp(18), dp(8))
    }
    private val choices = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val cancel = Button(context).apply {
        text = resources.getString(R.string.placement_cancel).uppercase()
        isAllCaps = false
        setOnClickListener { onCancel() }
    }
    private val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        elevation = dp(8).toFloat()
        addView(title, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(choices, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(96)))
        addView(cancel, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(50)))
    }
    private val buttons = KeyboardPlacementMode.entries.associateWith(::createModeButton)
    private var selected = KeyboardPlacementMode.STANDARD
    private var panelColor = Color.WHITE
    private var textColor = Color.BLACK
    private var accentColor = AdjustmentBlue

    var onModeSelected: (KeyboardPlacementMode) -> Unit = {}
    var onCancel: () -> Unit = {}

    init {
        isClickable = true
        setBackgroundColor(Color.argb(120, 0, 0, 0))
        buttons.values.forEach {
            choices.addView(it, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
            marginStart = dp(12)
            marginEnd = dp(12)
        })
        visibility = View.GONE
        applyColors()
    }

    fun render(mode: KeyboardPlacementMode) {
        selected = mode
        buttons.forEach { (item, button) -> button.isSelected = item == selected }
        applyColors()
    }

    fun updateTheme(theme: KeyboardTheme) {
        val palette = KeyboardPanelPalette.from(theme)
        panelColor = palette.backgroundEnd
        textColor = palette.readableOn(panelColor, Color.BLACK)
        accentColor = palette.accent
        applyColors()
    }

    private fun createModeButton(mode: KeyboardPlacementMode) = Button(context).apply {
        text = "${mode.icon()}\n${resources.getString(mode.labelRes())}"
        contentDescription = resources.getString(mode.labelRes())
        isAllCaps = false
        textSize = 13f
        setOnClickListener { onModeSelected(mode) }
    }

    private fun applyColors() {
        card.background = rounded(panelColor, 22)
        title.setTextColor(textColor)
        cancel.setTextColor(accentColor)
        cancel.background = rounded(panelColor, 0)
        buttons.forEach { (mode, button) ->
            val active = mode == selected
            val color = if (active) accentColor else panelColor
            button.background = rounded(color, 18)
            button.setTextColor(if (active) readableOnAccent(accentColor) else textColor)
        }
    }

    private fun KeyboardPlacementMode.labelRes() = when (this) {
        KeyboardPlacementMode.STANDARD -> R.string.placement_standard
        KeyboardPlacementMode.ELEVATED -> R.string.placement_elevated
        KeyboardPlacementMode.ONE_HANDED -> R.string.placement_one_handed
    }

    private fun KeyboardPlacementMode.icon() = when (this) {
        KeyboardPlacementMode.STANDARD -> "⌨"
        KeyboardPlacementMode.ELEVATED -> "↕"
        KeyboardPlacementMode.ONE_HANDED -> "◧"
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    private fun readableOnAccent(color: Int): Int =
        if (Color.luminance(color) > 0.55) Color.BLACK else Color.WHITE

    private fun dp(value: Int) = (value * density).roundToInt()

    private companion object { const val AdjustmentBlue = -0xF28701 }
}

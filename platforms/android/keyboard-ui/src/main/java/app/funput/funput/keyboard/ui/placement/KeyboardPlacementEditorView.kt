package app.funput.funput.keyboard.ui.placement

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Temporary controls shown at the bottom of the IME while placement is edited. */
internal class KeyboardPlacementEditorView(context: Context) : LinearLayout(context) {
    private val density = resources.displayMetrics.density
    private val standard = actionButton(R.string.placement_standard)
    private val elevated = actionButton(R.string.placement_elevated)
    private val done = actionButton(R.string.placement_done)
    private val reset = actionButton(R.string.placement_reset)
    private val slider = SeekBar(context)
    private var rendering = false
    private var preferences = KeyboardPlacementPreferences.Default

    var onModeSelected: (KeyboardPlacementMode) -> Unit = {}
    var onOffsetPreview: (Float) -> Unit = {}
    var onOffsetSettled: (Float) -> Unit = {}
    var onDone: () -> Unit = {}

    init {
        orientation = VERTICAL
        isClickable = true
        elevation = 8f * density
        setPadding(dp(8), dp(4), dp(8), dp(4))
        addView(row(standard, elevated, done), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(row(slider, reset), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        standard.setOnClickListener { onModeSelected(KeyboardPlacementMode.STANDARD) }
        elevated.setOnClickListener { onModeSelected(KeyboardPlacementMode.ELEVATED) }
        done.setOnClickListener { onDone() }
        reset.setOnClickListener {
            val value = KeyboardPlacementPreferences.DefaultElevatedOffsetDp
            onOffsetPreview(value)
            onOffsetSettled(value)
        }
        slider.setOnSeekBarChangeListener(SliderListener())
        visibility = View.GONE
    }

    fun render(value: KeyboardPlacementPreferences, maximumOffsetPx: Int) {
        preferences = value
        rendering = true
        slider.max = maximumOffsetPx.coerceAtLeast(1)
        slider.progress = (value.elevatedOffsetDp * density).roundToInt().coerceAtMost(slider.max)
        slider.isEnabled = value.activeMode == KeyboardPlacementMode.ELEVATED && maximumOffsetPx > 0
        slider.contentDescription = resources.getString(
            if (maximumOffsetPx > 0) R.string.placement_slider else R.string.placement_unavailable,
        )
        standard.isSelected = value.activeMode == KeyboardPlacementMode.STANDARD
        elevated.isSelected = value.activeMode == KeyboardPlacementMode.ELEVATED
        standard.alpha = if (standard.isSelected) 1f else 0.68f
        elevated.alpha = if (elevated.isSelected) 1f else 0.68f
        rendering = false
    }

    fun updateTheme(theme: KeyboardTheme) {
        val palette = KeyboardPanelPalette.from(theme)
        setBackgroundColor(palette.backgroundEnd)
        val foreground = palette.readable(palette.label)
        listOf(standard, elevated, done, reset).forEach { button ->
            button.setTextColor(foreground)
            button.backgroundTintList = ColorStateList.valueOf(palette.buttonSurface)
        }
        slider.thumbTintList = ColorStateList.valueOf(palette.accent)
        slider.progressTintList = ColorStateList.valueOf(palette.accent)
    }

    private fun actionButton(label: Int) = Button(context).apply {
        text = resources.getString(label)
        isAllCaps = false
        textSize = 12f
        minWidth = 0
        minimumWidth = 0
        setPadding(dp(6), 0, dp(6), 0)
    }

    private fun row(vararg children: View) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        children.forEach { child ->
            val weight = if (child === slider) 3f else 1f
            addView(child, LayoutParams(0, LayoutParams.MATCH_PARENT, weight))
        }
    }

    private fun dp(value: Int) = (value * density).roundToInt()

    private inner class SliderListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser && !rendering) onOffsetPreview(progress / density)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            if (!rendering) onOffsetSettled(seekBar.progress / density)
        }
    }
}

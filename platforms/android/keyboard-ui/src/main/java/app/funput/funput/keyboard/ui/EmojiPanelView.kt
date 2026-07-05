package app.funput.funput.keyboard.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.theme.KeyboardTheme

internal class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    var onEmojiSelected: (String) -> Unit = {}
    var onLettersRequested: () -> Unit = {}
    var onBackspaceRequested: (KeyAction) -> Unit = {}
    var hapticsEnabled: Boolean
        get() = isHapticFeedbackEnabled
        set(value) {
            isHapticFeedbackEnabled = value
            picker.hapticsEnabled = value
        }
    var soundsEnabled: Boolean
        get() = isSoundEffectsEnabled
        set(value) { isSoundEffectsEnabled = value }

    private val picker = ScrollableEmojiPickerView(
        ContextThemeWrapper(context, R.style.Theme_Funput_EmojiPicker),
    )
    private val toolbar = EmojiPanelToolbar(context)

    init {
        orientation = VERTICAL
        picker.setBackgroundColor(Color.TRANSPARENT)
        picker.setOnEmojiPickedListener { emoji ->
            haptic(KeyboardHapticType.KEY_PRESS)
            sound()
            onEmojiSelected(emoji)
        }
        toolbar.onLettersRequested = {
            haptic(KeyboardHapticType.CONTROL)
            sound()
            onLettersRequested()
        }
        toolbar.onBackspaceRequested = {
            haptic(KeyboardHapticType.DELETE)
            sound()
            onBackspaceRequested(KeyAction.Backspace)
        }
        addView(picker, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(pickerDivider(), LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))
        addView(toolbar, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)))
    }

    fun updateTheme(theme: KeyboardTheme) {
        background = EmojiPanelBackgrounds.panel(theme)
        toolbar.updateTheme(theme)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        val widthDp = width / resources.displayMetrics.density
        picker.emojiGridColumns = EmojiGridColumns.forWidth(widthDp)
    }

    private fun pickerDivider() = android.view.View(context).apply {
        setBackgroundColor(0x33FFFFFF)
    }

    private fun haptic(type: KeyboardHapticType) {
        KeyboardHaptics.perform(this, type)
    }

    private fun sound() = KeyboardSounds.perform(this)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

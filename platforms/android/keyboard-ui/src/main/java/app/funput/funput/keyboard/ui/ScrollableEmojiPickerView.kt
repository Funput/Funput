package app.funput.funput.keyboard.ui

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.emoji2.emojipicker.EmojiPickerView

/** Keeps vertical picker gestures inside the keyboard when hosted by a scrollable preview. */
internal class ScrollableEmojiPickerView(context: Context) : FrameLayout(context) {
    private val picker = EmojiPickerView(context)

    var emojiGridColumns: Int
        get() = picker.emojiGridColumns
        set(value) {
            picker.emojiGridColumns = value
        }

    init {
        picker.setBackgroundColor(Color.TRANSPARENT)
        addView(picker, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setOnEmojiPickedListener(listener: (String) -> Unit) {
        picker.setOnEmojiPickedListener { item -> listener(item.emoji) }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled
    }
}

package app.funput.funput.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Complete Funput keyboard UI, including panel navigation and host callbacks. */
class FunputKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    val callbacks = FunputKeyboardCallbacks()
    val activePanel: KeyboardPanel get() = panelController.activePanel
    val shiftState: ShiftState get() = lettersSurface.shiftState

    var inputMethod: KeyboardInputMethod
        get() = lettersSurface.inputMethod
        set(value) {
            lettersSurface.inputMethod = value
            requestLayout()
        }

    var keyboardTheme: KeyboardTheme
        get() = lettersSurface.keyboardTheme
        set(value) {
            lettersSurface.keyboardTheme = value
            emojiPanel?.updateTheme(value)
        }

    var suggestions: List<String>
        get() = lettersSurface.suggestions
        set(value) {
            lettersSurface.suggestions = value
        }

    var language: KeyboardLanguage
        get() = lettersSurface.language
        set(value) {
            lettersSurface.language = value
        }

    private val panelController = KeyboardPanelController()
    private val lettersSurface = KeyboardSurfaceView(context)
    private var emojiPanel: EmojiPanelView? = null

    init {
        addView(lettersSurface, matchParentLayoutParams())
        lettersSurface.callbacks.onKeyAction = callbacks::dispatch
        lettersSurface.callbacks.onSuggestionSelected = callbacks::dispatchSuggestion
        lettersSurface.callbacks.onEmojiRequested = ::openEmojiFromKeyboard
    }

    fun showEmojiPanel() {
        if (!panelController.show(KeyboardPanel.EMOJI)) return
        val panel = emojiPanel ?: createEmojiPanel().also {
            emojiPanel = it
            addView(it, matchParentLayoutParams())
        }
        lettersSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
    }

    fun showLettersPanel() {
        if (!panelController.show(KeyboardPanel.LETTERS)) return
        emojiPanel?.visibility = View.GONE
        lettersSurface.visibility = View.VISIBLE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val width = resolveSize((DefaultWidthDp * density).roundToInt(), widthMeasureSpec)
        val height = resolveSize((recommendedHeightDp(inputMethod) * density).roundToInt(), heightMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
    }

    private fun openEmojiFromKeyboard() {
        showEmojiPanel()
        callbacks.dispatchEmojiPanelOpened()
    }

    private fun createEmojiPanel() = EmojiPanelView(context).apply {
        updateTheme(keyboardTheme)
        onEmojiSelected = callbacks::dispatchEmoji
        onBackspaceRequested = callbacks::dispatch
        onLettersRequested = ::showLettersPanel
    }

    private fun matchParentLayoutParams() = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT,
    )

    companion object {
        private const val DefaultWidthDp = 360f

        fun recommendedHeightDp(inputMethod: KeyboardInputMethod): Float =
            KeyboardSurfaceView.recommendedHeightDp(inputMethod)
    }
}

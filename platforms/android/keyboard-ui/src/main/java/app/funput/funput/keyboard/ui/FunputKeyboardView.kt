package app.funput.funput.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
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
    var shiftState: ShiftState
        get() = keyboardSurface.shiftState
        set(value) { keyboardSurface.shiftState = value }
    var inputMethod: KeyboardInputMethod
        get() = keyboardSurface.inputMethod
        set(value) {
            keyboardSurface.inputMethod = value
            requestLayout()
        }
    var editorMode: KeyboardEditorMode
        get() = keyboardSurface.editorMode
        set(value) {
            keyboardSurface.editorMode = value
            requestLayout()
        }
    var suggestionBarEnabled: Boolean
        get() = keyboardSurface.suggestionBarEnabled
        set(value) {
            keyboardSurface.suggestionBarEnabled = value
            syncSuggestions()
        }
    var enterAction: KeyboardEnterAction
        get() = keyboardSurface.enterAction
        set(value) { keyboardSurface.enterAction = value }
    var keyboardTheme: KeyboardTheme
        get() = keyboardSurface.keyboardTheme
        set(value) {
            keyboardSurface.keyboardTheme = value
            emojiPanel?.updateTheme(value)
        }
    var suggestions: List<String> = emptyList()
        set(value) {
            field = value
            syncSuggestions()
        }
    var language: KeyboardLanguage
        get() = keyboardSurface.language
        set(value) { keyboardSurface.language = value }
    var hapticsEnabled: Boolean
        get() = feedbackController.hapticsEnabled
        set(value) { feedbackController.hapticsEnabled = value }
    var soundsEnabled: Boolean
        get() = feedbackController.soundsEnabled
        set(value) { feedbackController.soundsEnabled = value }
    private val panelController = KeyboardPanelController()
    private val keyboardSurface = KeyboardSurfaceView(context)
    private var emojiPanel: EmojiPanelView? = null
    private val feedbackController = KeyboardFeedbackController(keyboardSurface) { emojiPanel }
    init {
        addView(keyboardSurface, matchParentLayoutParams())
        keyboardSurface.callbacks.onKeyAction = ::handleKeyAction
        keyboardSurface.callbacks.onSuggestionSelected = callbacks::dispatchSuggestion
        keyboardSurface.callbacks.onEmojiRequested = ::openEmojiFromKeyboard
    }

    fun showEmojiPanel() {
        if (!panelController.show(KeyboardPanel.EMOJI)) return
        val panel = emojiPanel ?: createEmojiPanel().also {
            emojiPanel = it
            addView(it, matchParentLayoutParams())
        }
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
    }

    fun showSymbolsPanel(mode: KeyboardLayoutMode = KeyboardLayoutMode.SYMBOLS_PRIMARY) {
        require(mode != KeyboardLayoutMode.LETTERS) { "Symbols panel requires a symbols layout" }
        panelController.show(KeyboardPanel.SYMBOLS)
        emojiPanel?.visibility = View.GONE
        keyboardSurface.layoutMode = mode
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
    }

    fun showLettersPanel() {
        if (!panelController.show(KeyboardPanel.LETTERS)) return
        emojiPanel?.visibility = View.GONE
        keyboardSurface.layoutMode = KeyboardLayoutMode.LETTERS
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val width = resolveSize((KeyboardDimensions.DefaultWidthDp * density).roundToInt(), widthMeasureSpec)
        val heightDp = KeyboardDimensions.recommendedHeightDp(inputMethod, editorMode)
        val height = resolveSize((heightDp * density).roundToInt(), heightMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
    }

    private fun openEmojiFromKeyboard() {
        if (editorMode.isPassword) return
        showEmojiPanel()
        callbacks.dispatchEmojiPanelOpened()
    }

    private fun handleKeyAction(action: KeyAction) = when (action) {
        KeyAction.Symbols -> showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_PRIMARY)
        KeyAction.MoreSymbols -> showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_SECONDARY)
        KeyAction.Letters -> showLettersPanel()
        else -> callbacks.dispatch(action)
    }

    private fun syncSuggestions() {
        val visible = activePanel == KeyboardPanel.LETTERS && suggestionBarEnabled
        keyboardSurface.suggestions = suggestions.takeIf { visible }.orEmpty()
    }

    private fun createEmojiPanel() = EmojiPanelView(context).apply {
        updateTheme(keyboardTheme)
        hapticsEnabled = this@FunputKeyboardView.hapticsEnabled
        soundsEnabled = this@FunputKeyboardView.soundsEnabled
        onEmojiSelected = callbacks::dispatchEmoji
        onBackspaceRequested = callbacks::dispatch
        onLettersRequested = ::showLettersPanel
    }

    private fun matchParentLayoutParams() =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

}

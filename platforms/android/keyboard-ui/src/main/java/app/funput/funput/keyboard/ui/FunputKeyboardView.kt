package app.funput.funput.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
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
    private val panelController = KeyboardPanelController()
    private val keyboardSurface = KeyboardSurfaceView(context)
    private var emojiPanel: EmojiPanelView? = null
    private val feedbackController = KeyboardFeedbackController(keyboardSurface) { emojiPanel }
    val callbacks = FunputKeyboardCallbacks()
    val activePanel: KeyboardPanel get() = panelController.activePanel
    var shiftState: ShiftState by keyboardSurface::shiftState
    var inputMethod: KeyboardInputMethod by keyboardSurface::inputMethod
    var editorMode: KeyboardEditorMode by keyboardSurface::editorMode
    var systemInputMethodSwitcherVisible: Boolean by keyboardSurface::systemInputMethodSwitcherVisible
    var suggestionBarEnabled: Boolean
        get() = keyboardSurface.suggestionBarEnabled
        set(value) {
            keyboardSurface.suggestionBarEnabled = value
            syncSuggestions()
        }
    var enterAction: KeyboardEnterAction by keyboardSurface::enterAction
    var keyboardTheme: KeyboardTheme
        get() = keyboardSurface.keyboardTheme
        set(value) {
            keyboardSurface.keyboardTheme = value
            emojiPanel?.updateTheme(value)
            setBackgroundColor(value.backgroundEndColor)
        }
    var keyboardThemeBackgroundImage by keyboardSurface::keyboardThemeBackgroundImage
    var sizingProfile: KeyboardSizingProfile by keyboardSurface::sizingProfile
    var suggestions: List<String> = emptyList()
        set(value) {
            field = value
            syncSuggestions()
        }
    var language: KeyboardLanguage by keyboardSurface::language
    var hapticsEnabled: Boolean by feedbackController::hapticsEnabled
    var soundsEnabled: Boolean by feedbackController::soundsEnabled
    private val safeArea = KeyboardSafeAreaController(this)
    init {
        addView(keyboardSurface, matchParentLayoutParams())
        keyboardSurface.callbacks.onKeyAction = ::handleKeyAction
        keyboardSurface.callbacks.onSettingsRequested = ::openSettings
        keyboardSurface.callbacks.onSuggestionSelected = callbacks::dispatchSuggestion
        keyboardSurface.callbacks.onEmojiRequested = ::openEmojiFromKeyboard
        setBackgroundColor(keyboardTheme.backgroundEndColor)
        safeArea.install()
    }

    fun showEmojiPanel() {
        if (!panelController.show(KeyboardPanel.EMOJI)) return
        val panel = emojiPanel ?: createEmojiPanel().also {
            emojiPanel = it
            addView(it, matchParentLayoutParams())
        }
        keyboardSurface.visibility = View.GONE
        panel.visibility = View.VISIBLE
        callbacks.dispatchPanelChanged(KeyboardPanel.EMOJI)
    }

    fun showSymbolsPanel(mode: KeyboardLayoutMode = KeyboardLayoutMode.SYMBOLS_PRIMARY) {
        require(mode != KeyboardLayoutMode.LETTERS) { "Symbols panel requires a symbols layout" }
        panelController.show(KeyboardPanel.SYMBOLS)
        emojiPanel?.visibility = View.GONE
        keyboardSurface.layoutMode = mode
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        callbacks.dispatchPanelChanged(KeyboardPanel.SYMBOLS)
    }

    fun showLettersPanel() {
        if (!panelController.show(KeyboardPanel.LETTERS)) return
        emojiPanel?.visibility = View.GONE
        keyboardSurface.layoutMode = KeyboardLayoutMode.LETTERS
        keyboardSurface.visibility = View.VISIBLE
        syncSuggestions()
        callbacks.dispatchPanelChanged(KeyboardPanel.LETTERS)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val keyboardWidth = (KeyboardDimensions.DefaultWidthDp * density).roundToInt()
        val width = resolveSize(keyboardWidth + safeArea.horizontalInset, widthMeasureSpec)
        val heightDp = KeyboardDimensions.recommendedHeightDp(
            inputMethod = inputMethod,
            editorMode = editorMode,
            profile = sizingProfile,
            widthDp = (width - safeArea.horizontalInset) / density,
        )
        val height = resolveSize((heightDp * density).roundToInt() + safeArea.bottomInset, heightMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
    }

    private fun openSettings() = context.openFunputSettings()

    private fun openEmojiFromKeyboard() {
        if (editorMode.isPassword) return
        showEmojiPanel()
        callbacks.dispatchEmojiPanelOpened()
    }

    private fun handleKeyAction(action: KeyAction) = when (action) {
        KeyAction.Symbols -> showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_PRIMARY)
        KeyAction.MoreSymbols -> showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_SECONDARY)
        KeyAction.Letters -> showLettersPanel()
        KeyAction.SwitchInputMethod -> callbacks.dispatchInputMethodSwitchRequest()
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

    private fun matchParentLayoutParams() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
}

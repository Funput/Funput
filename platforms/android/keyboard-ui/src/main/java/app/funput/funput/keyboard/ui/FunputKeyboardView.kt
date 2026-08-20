package app.funput.funput.keyboard.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.ui.panel.KeyboardPanelCoordinator
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.panel.FunputPanelFactory
import app.funput.funput.keyboard.ui.panel.KeyboardClipboardPanelState
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Complete Funput keyboard UI, including panel navigation and host callbacks. */
class FunputKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val keyboardSurface = KeyboardSurfaceView(context)
    val callbacks = FunputKeyboardCallbacks()
    private val clipboardState = KeyboardClipboardPanelState(
        keyboardSurface, { editorMode }, { activePanel }, ::showLettersPanel,
    )
    private val panelFactory = FunputPanelFactory(
        context, callbacks, { keyboardSurface.keyboardTheme },
        { keyboardSurface.isHapticFeedbackEnabled }, { keyboardSurface.isSoundEffectsEnabled },
        clipboardState, ::showLettersPanel,
    )
    private val panelCoordinator = KeyboardPanelCoordinator(
        keyboardSurface = keyboardSurface,
        createEmojiPanel = panelFactory::createEmoji,
        createClipboardPanel = panelFactory::createClipboard,
        attachPanel = { addView(it, matchParentLayoutParams()) },
        onPanelChanged = callbacks::dispatchPanelChanged,
        syncSuggestions = ::syncSuggestions,
    )
    private val feedbackController = KeyboardFeedbackController(
        keyboardSurface, { panelCoordinator.loadedEmojiPanel }, { panelCoordinator.loadedClipboardPanel },
    )
    val activePanel: KeyboardPanel get() = panelCoordinator.activePanel
    var shiftState: ShiftState by keyboardSurface::shiftState
    var inputMethod: KeyboardInputMethod by keyboardSurface::inputMethod
    var editorMode: KeyboardEditorMode
        get() = keyboardSurface.editorMode
        set(value) { keyboardSurface.editorMode = value; clipboardState.editorModeChanged() }
    var systemInputMethodSwitcherVisible: Boolean by keyboardSurface::systemInputMethodSwitcherVisible
    var showsNumberRow: Boolean by keyboardSurface::showsNumberRow
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
            panelCoordinator.updateTheme(value)
            setBackgroundColor(value.backgroundEndColor)
        }
    var keyboardThemeBackgroundImage by keyboardSurface::keyboardThemeBackgroundImage
    var sizingProfile: KeyboardSizingProfile by keyboardSurface::sizingProfile
    var suggestions: List<String> = emptyList()
        set(value) {
            field = value
            syncSuggestions()
        }
    var clipboardHint: KeyboardClipboardHint? by keyboardSurface::clipboardHint
    var clipboardPanelEnabled: Boolean by clipboardState::enabled
    var clipboardEntries: List<KeyboardClipboardEntry> by clipboardState::entries
    var clipboardHistoryLoading: Boolean by clipboardState::loading
    var language: KeyboardLanguage by keyboardSurface::language
    var areSmartGesturesEnabled by keyboardSurface::areSmartGesturesEnabled
    var hapticsEnabled: Boolean by feedbackController::hapticsEnabled
    var soundsEnabled: Boolean by feedbackController::soundsEnabled
    private val safeArea = KeyboardSafeAreaController(this)
    init { KeyboardComposeLifecycle.install(this)
        addView(keyboardSurface, matchParentLayoutParams())
        keyboardSurface.callbacks.onKeyAction = ::handleKeyAction
        keyboardSurface.callbacks.onSuggestionSelected = callbacks::dispatchSuggestion
        keyboardSurface.callbacks.onEmojiRequested = ::openEmojiFromKeyboard
        keyboardSurface.callbacks.onClipboardPasteRequested = callbacks::dispatchClipboardPasteRequest
        keyboardSurface.callbacks.onClipboardPanelRequested = ::showClipboardPanel
        setBackgroundColor(keyboardTheme.backgroundEndColor)
        safeArea.install()
    }

    fun showEmojiPanel(): Unit = panelCoordinator.showEmoji()
    fun showClipboardPanel() {
        if (!clipboardState.available() || activePanel == KeyboardPanel.CLIPBOARD) return
        panelCoordinator.showClipboard()
        callbacks.dispatchClipboardPanelOpened()
    }

    fun showSymbolsPanel(mode: KeyboardLayoutMode = KeyboardLayoutMode.SYMBOLS_PRIMARY) {
        panelCoordinator.showSymbols(mode)
    }

    fun showLettersPanel(): Unit = panelCoordinator.showLetters()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val keyboardWidth = (KeyboardDimensions.DefaultWidthDp * density).roundToInt()
        val width = resolveSize(keyboardWidth + safeArea.horizontalInset, widthMeasureSpec)
        val contentWidthDp = (width - safeArea.horizontalInset) / density
        val heightDp = KeyboardDimensions.recommendedHeightDp(
            inputMethod, editorMode, sizingProfile, contentWidthDp, showsNumberRow,
        )
        val height = resolveSize((heightDp * density).roundToInt() + safeArea.bottomInset, heightMeasureSpec)
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
        KeyAction.SwitchInputMethod -> callbacks.dispatchInputMethodSwitchRequest()
        else -> callbacks.dispatch(action)
    }

    private fun syncSuggestions() {
        val visible = activePanel == KeyboardPanel.LETTERS && suggestionBarEnabled
        keyboardSurface.suggestions = suggestions.takeIf { visible }.orEmpty()
    }

    private fun matchParentLayoutParams() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
}

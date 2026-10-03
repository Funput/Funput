package app.funput.funput.keyboard.ui

import android.content.Context
import app.funput.funput.keyboard.ui.host.bindKeyboardToolbar
import app.funput.funput.keyboard.ui.speech.KeyboardSpeechPanelBinding
import android.util.AttributeSet
import android.widget.FrameLayout
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.ui.host.KeyboardHostMeasure
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.ui.panel.KeyboardPanelCoordinator
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.localtext.LocalTextFields
import app.funput.funput.keyboard.ui.panel.FunputPanelFactory
import app.funput.funput.keyboard.ui.panel.KeyboardClipboardPanelState
import app.funput.funput.keyboard.ui.placement.KeyboardPlacementHostController
import app.funput.funput.theme.KeyboardTheme

/** Complete Funput keyboard UI, including panel navigation and host callbacks. */
class FunputKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val keyboardSurface = KeyboardSurfaceView(context)
    private val contentHost = FrameLayout(context)
    val callbacks = FunputKeyboardCallbacks()
    private val clipboardState = KeyboardClipboardPanelState(
        keyboardSurface, { editorMode }, { activePanel }, ::showLettersPanel,
    )
    val localTextFields = LocalTextFields()
    private val panelFactory = FunputPanelFactory(
        context, callbacks, { keyboardSurface.keyboardTheme },
        { keyboardSurface.isHapticFeedbackEnabled }, { keyboardSurface.isSoundEffectsEnabled },
        clipboardState, localTextFields, ::showLettersPanel,
    )
    private val speech = KeyboardSpeechPanelBinding(context, keyboardSurface, callbacks, ::showLettersPanel)
    private val panelCoordinator = KeyboardPanelCoordinator(
        keyboardSurface = keyboardSurface,
        createSpeechPanel = speech::create,
        onSpeechAction = callbacks::dispatchSpeechAction,
        clearSpeech = speech::clear,
        createEmojiPanel = panelFactory::createEmoji,
        createClipboardPanel = panelFactory::createClipboard,
        attachPanel = { contentHost.addView(it, matchParentLayoutParams()) },
        onPanelChanged = callbacks::dispatchPanelChanged,
        syncSuggestions = ::syncSuggestions,
    )
    private val feedbackController = KeyboardFeedbackController(
        keyboardSurface, { panelCoordinator.loadedEmojiPanel }, { panelCoordinator.loadedClipboardPanel },
        speech::updateFeedback,
    )
    val activePanel: KeyboardPanel get() = panelCoordinator.activePanel
    var shiftState: ShiftState by keyboardSurface::shiftState
    var inputMethod: KeyboardInputMethod by keyboardSurface::inputMethod
    var editorMode: KeyboardEditorMode
        get() = keyboardSurface.editorMode
        set(value) { keyboardSurface.editorMode = value; clipboardState.editorModeChanged() }
    var systemInputMethodSwitcherVisible: Boolean by keyboardSurface::systemInputMethodSwitcherVisible
    var microphone by keyboardSurface::microphone
    var speechPanelState by speech::state
    var placementKeyVisible: Boolean by keyboardSurface::placementKeyVisible
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
            placement.updateTheme(value)
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
    private val placement = KeyboardPlacementHostController(this, contentHost, safeArea, callbacks)
    private val hostMeasure = KeyboardHostMeasure(this, safeArea, placement)
    var placementPreferences: KeyboardPlacementPreferences by placement::preferences
    init { KeyboardComposeLifecycle.install(this)
        addView(contentHost, matchParentLayoutParams())
        contentHost.addView(keyboardSurface, matchParentLayoutParams())
        keyboardSurface.callbacks.onKeyAction = ::routeKeyAction
        keyboardSurface.callbacks.onSuggestionSelected = callbacks::dispatchSuggestion
        bindKeyboardToolbar(keyboardSurface, callbacks, ::openEmojiFromKeyboard,
            ::showClipboardPanel, placement::showPicker)
        setBackgroundColor(keyboardTheme.backgroundEndColor)
        safeArea.install()
    }

    fun showSpeechPanel(): Unit = panelCoordinator.showSpeech()
    fun showEmojiPanel(): Unit = panelCoordinator.showEmoji()
    fun showClipboardPanel() {
        if (!clipboardState.available() || activePanel == KeyboardPanel.CLIPBOARD) return
        panelCoordinator.showClipboard(); callbacks.dispatchClipboardPanelOpened()
    }

    fun showSymbolsPanel(mode: KeyboardLayoutMode = KeyboardLayoutMode.SYMBOLS_PRIMARY) =
        panelCoordinator.showSymbols(mode)

    fun showLettersPanel(): Unit = panelCoordinator.showLetters()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = hostMeasure.resolve(widthMeasureSpec, heightMeasureSpec,
            inputMethod, editorMode, sizingProfile, showsNumberRow)
        super.onMeasure(size.widthSpec, size.heightSpec)
    }

    private fun openEmojiFromKeyboard() {
        if (editorMode.isPassword) return
        showEmojiPanel(); callbacks.dispatchEmojiPanelOpened()
    }

    private fun syncSuggestions() {
        val visible = activePanel == KeyboardPanel.LETTERS && suggestionBarEnabled
        keyboardSurface.suggestions = suggestions.takeIf { visible }.orEmpty()
    }

    private fun matchParentLayoutParams() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
}

package app.funput.funput.keyboard.ui.emoji.panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.emoji.EmojiBottomBarView
import app.funput.funput.keyboard.ui.emoji.EmojiBrowserView
import app.funput.funput.keyboard.ui.emoji.EmojiCatalog
import app.funput.funput.keyboard.ui.emoji.EmojiCatalogLoader
import app.funput.funput.keyboard.ui.emoji.EmojiItem
import app.funput.funput.keyboard.ui.emoji.EmojiLoadingView
import app.funput.funput.keyboard.ui.emoji.EmojiRecentsStore
import app.funput.funput.keyboard.ui.emoji.EmojiSearchContentView
import app.funput.funput.keyboard.ui.emoji.EmojiSearchController
import app.funput.funput.keyboard.ui.emoji.EmojiSearchHeaderView
import app.funput.funput.keyboard.ui.emoji.EmojiSearchIndex
import app.funput.funput.keyboard.ui.emoji.EmojiSearchMode
import app.funput.funput.keyboard.ui.emoji.EmojiSearchState
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme

internal class EmojiBrowserPanelView(context: Context) : LinearLayout(context) {
    var onEmojiSelected: (String) -> Unit = {}
    var onLettersRequested: () -> Unit = {}
    var onKaomojiRequested: () -> Unit = {}
    var onBackspaceRequested: (KeyAction) -> Unit = {}
    var hapticsEnabled: Boolean
        get() = isHapticFeedbackEnabled
        set(value) {
            isHapticFeedbackEnabled = value
            search.updateFeedback(value, soundsEnabled)
        }
    var soundsEnabled: Boolean
        get() = isSoundEffectsEnabled
        set(value) {
            isSoundEffectsEnabled = value
            search.updateFeedback(hapticsEnabled, value)
        }
    private val header = EmojiSearchHeaderView(context)
    private val browser = EmojiBrowserView(context)
    private val search = EmojiSearchContentView(context)
    private val bottom = EmojiBottomBarView(context)
    private val loading = EmojiLoadingView(context)
    private val content = FrameLayout(context)
    private val divider = View(context)
    private val recents = EmojiRecentsStore(context)
    private var catalog = EmojiCatalog.Empty
    private var index = EmojiSearchIndex(emptyList())
    private lateinit var palette: KeyboardPanelPalette
    private val controller = EmojiSearchController(::renderSearch)

    init {
        orientation = VERTICAL
        content.addView(browser, matchParent())
        content.addView(search, matchParent())
        content.addView(loading, matchParent())
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(46)))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))
        addView(bottom, LayoutParams(LayoutParams.MATCH_PARENT, dp(46)))
        wireActions()
        EmojiCatalogLoader.load(context) { loaded ->
            catalog = loaded.catalog
            index = loaded.searchIndex
            if (catalog.emojis.isEmpty()) loading.showEmpty()
            loading.visibility = if (catalog.emojis.isEmpty()) VISIBLE else GONE
            refreshBrowser()
            renderSearch(controller.state)
        }
    }

    fun updateTheme(theme: KeyboardTheme) {
        palette = KeyboardPanelPalette.from(theme)
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(palette.backgroundStart, palette.backgroundEnd),
        )
        divider.setBackgroundColor(palette.divider)
        loading.updatePalette(palette)
        header.render(controller.state, palette)
        browser.updatePalette(palette)
        bottom.updatePalette(palette)
        search.updateTheme(theme)
        renderSearch(controller.state)
    }

    fun reset() {
        controller.reset()
        browser.reset()
    }

    private fun wireActions() {
        header.onSearchRequested = controller::begin
        header.onClearRequested = controller::clear
        header.onCancelRequested = controller::cancel
        browser.onEmojiSelected = ::select
        browser.onCategoryChanged = bottom::setSelected
        bottom.onCategoryRequested = browser::scrollTo
        bottom.onLettersRequested = { feedback(KeyboardHapticType.CONTROL); onLettersRequested() }
        bottom.onKaomojiRequested = { feedback(KeyboardHapticType.CONTROL); onKaomojiRequested() }
        bottom.onBackspaceRequested = { feedback(KeyboardHapticType.DELETE); onBackspaceRequested(KeyAction.Backspace) }
        search.onEmojiSelected = ::select
        search.onInput = controller::input
        search.onSpace = controller::space
        search.onBackspace = controller::backspace
        search.onDone = controller::done
        search.onCancel = controller::cancel
    }

    private fun select(item: EmojiItem) {
        feedback(KeyboardHapticType.KEY_PRESS)
        recents.record(item.glyph)
        onEmojiSelected(item.glyph)
        refreshBrowser()
    }

    private fun refreshBrowser() = browser.submit(catalog, recents.glyphs())

    private fun renderSearch(state: EmojiSearchState) {
        if (!::palette.isInitialized) return
        val browsing = state.mode == EmojiSearchMode.BROWSING
        browser.visibility = if (browsing) VISIBLE else GONE
        search.visibility = if (browsing) GONE else VISIBLE
        bottom.visibility = if (browsing) VISIBLE else GONE
        divider.visibility = bottom.visibility
        header.render(state, palette)
        search.render(state, index.search(state.query), palette)
    }

    private fun feedback(type: KeyboardHapticType) {
        KeyboardHaptics.perform(this, type)
        KeyboardSounds.perform(this, type)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun matchParent() = FrameLayout.LayoutParams(-1, -1)
}

package app.funput.funput.keyboard.ui.kaomoji.panel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.ui.kaomoji.browser.KaomojiBrowserView
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCatalog
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCatalogLoader
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiItem
import app.funput.funput.keyboard.ui.kaomoji.persistence.KaomojiRecentsStore
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme

internal class KaomojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    var onKaomojiSelected: (String) -> Unit = {}
    var onLettersRequested: () -> Unit = {}
    var onEmojiRequested: () -> Unit = {}
    var onBackspaceRequested: (KeyAction) -> Unit = {}
    var hapticsEnabled: Boolean
        get() = isHapticFeedbackEnabled
        set(value) { isHapticFeedbackEnabled = value }
    var soundsEnabled: Boolean
        get() = isSoundEffectsEnabled
        set(value) { isSoundEffectsEnabled = value }
    private val browser = KaomojiBrowserView(context)
    private val bottom = KaomojiBottomBarView(context)
    private val divider = View(context)
    private val recents = KaomojiRecentsStore(context)
    private var catalog = KaomojiCatalog.Empty

    init {
        orientation = VERTICAL
        addView(browser, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, dp(1)))
        addView(bottom, LayoutParams(LayoutParams.MATCH_PARENT, dp(46)))
        wireActions()
        KaomojiCatalogLoader.load(context) {
            catalog = it
            refreshBrowser()
        }
    }

    fun updateTheme(theme: KeyboardTheme) {
        val palette = KeyboardPanelPalette.from(theme)
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(palette.backgroundStart, palette.backgroundEnd),
        )
        divider.setBackgroundColor(palette.divider)
        browser.updatePalette(palette)
        bottom.updatePalette(palette)
    }

    fun reset() = browser.reset()

    private fun wireActions() {
        browser.onKaomojiSelected = ::select
        browser.onCategoryChanged = bottom::setSelected
        bottom.onCategoryRequested = browser::scrollTo
        bottom.onLettersRequested = { feedback(KeyboardHapticType.CONTROL); onLettersRequested() }
        bottom.onEmojiRequested = { feedback(KeyboardHapticType.CONTROL); onEmojiRequested() }
        bottom.onBackspaceRequested = { feedback(KeyboardHapticType.DELETE); onBackspaceRequested(KeyAction.Backspace) }
    }

    private fun select(item: KaomojiItem) {
        feedback(KeyboardHapticType.KEY_PRESS)
        recents.record(item.text)
        onKaomojiSelected(item.text)
        refreshBrowser()
    }

    private fun refreshBrowser() = browser.submit(catalog, recents.texts())

    private fun feedback(type: KeyboardHapticType) {
        KeyboardHaptics.perform(this, type)
        KeyboardSounds.perform(this, type)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

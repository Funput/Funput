package app.funput.funput.keyboard.ui

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardPanelCoordinatorInstrumentedTest {
    @Test
    fun panelsAreLazyReusedAndKeepExistingTransitionSemantics() = withKeyboard { keyboard ->
        val changes = mutableListOf<KeyboardPanel>()
        keyboard.callbacks.onPanelChanged = changes::add
        assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)
        assertEquals(1, keyboard.childCount)

        keyboard.showEmojiPanel()
        val firstPanel = keyboard.childOfType<EmojiPanelView>()
        assertEquals(2, keyboard.childCount)
        keyboard.showEmojiPanel()
        assertEquals(listOf(KeyboardPanel.EMOJI), changes)

        firstPanel.showKaomojiPanel()
        keyboard.showLettersPanel()
        keyboard.showEmojiPanel()
        assertSame(firstPanel, keyboard.childOfType<EmojiPanelView>())
        assertFalse(firstPanel.isShowingKaomoji)

        keyboard.showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_PRIMARY)
        keyboard.showSymbolsPanel(KeyboardLayoutMode.SYMBOLS_SECONDARY)
        assertEquals(KeyboardLayoutMode.SYMBOLS_SECONDARY, keyboard.surface().layoutMode)
        assertEquals(
            listOf(
                KeyboardPanel.EMOJI,
                KeyboardPanel.LETTERS,
                KeyboardPanel.EMOJI,
                KeyboardPanel.SYMBOLS,
                KeyboardPanel.SYMBOLS,
            ),
            changes,
        )
        assertEquals(View.GONE, firstPanel.visibility)
        assertEquals(View.VISIBLE, keyboard.surface().visibility)
    }

    @Test
    fun toolbarRequestIsBlockedForSecureEditorsButPublicApiIsPreserved() = withKeyboard { keyboard ->
        keyboard.editorMode = KeyboardEditorMode.PASSWORD
        keyboard.surface().callbacks.onEmojiRequested?.invoke()
        assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)

        keyboard.editorMode = KeyboardEditorMode.PIN
        keyboard.surface().callbacks.onEmojiRequested?.invoke()
        assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)

        keyboard.showEmojiPanel()
        assertEquals(KeyboardPanel.EMOJI, keyboard.activePanel)
    }

    @Test
    fun suggestionsAndFeedbackFollowPanelLifecycle() = withKeyboard { keyboard ->
        keyboard.keyboardTheme = KeyboardThemes.GlassDark
        keyboard.suggestionBarEnabled = true
        keyboard.suggestions = listOf("xin", "chào")
        assertEquals(keyboard.suggestions, keyboard.surface().suggestions)

        keyboard.showSymbolsPanel()
        assertTrue(keyboard.surface().suggestions.isEmpty())
        keyboard.showLettersPanel()
        assertEquals(keyboard.suggestions, keyboard.surface().suggestions)

        keyboard.hapticsEnabled = false
        keyboard.soundsEnabled = false
        keyboard.showEmojiPanel()
        val panel = keyboard.childOfType<EmojiPanelView>()
        panel.assertTheme(KeyboardThemes.GlassDark)
        assertFalse(panel.hapticsEnabled)
        assertFalse(panel.soundsEnabled)
        keyboard.keyboardTheme = KeyboardThemes.GlassLight
        keyboard.hapticsEnabled = true
        keyboard.soundsEnabled = true
        panel.assertTheme(KeyboardThemes.GlassLight)
        assertTrue(panel.hapticsEnabled)
        assertTrue(panel.soundsEnabled)
    }

    @Test
    fun symbolsRejectsLettersLayout() = withKeyboard { keyboard ->
        assertThrows(IllegalArgumentException::class.java) {
            keyboard.showSymbolsPanel(KeyboardLayoutMode.LETTERS)
        }
        assertEquals(KeyboardPanel.LETTERS, keyboard.activePanel)
    }

    private fun withKeyboard(assertions: (FunputKeyboardView) -> Unit) {
        ActivityScenario.launch(EmojiTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                FunputKeyboardView(activity).also {
                    activity.setContentView(it)
                    it.measure(exactly(1080), exactly(600))
                    it.layout(0, 0, 1080, 600)
                    assertions(it)
                }
            }
        }
    }

    private fun FunputKeyboardView.surface() = childOfType<KeyboardSurfaceView>()

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)

    private fun EmojiPanelView.assertTheme(theme: KeyboardTheme) {
        val palette = KeyboardPanelPalette.from(theme)
        val expected = intArrayOf(palette.backgroundStart, palette.backgroundEnd)
        repeat(childCount) { index ->
            val background = getChildAt(index).background as GradientDrawable
            assertArrayEquals(expected, background.colors)
        }
    }

    private inline fun <reified T : View> ViewGroup.childOfType(): T =
        (0 until childCount).asSequence()
            .map(::getChildAt)
            .filterIsInstance<T>()
            .first()
}

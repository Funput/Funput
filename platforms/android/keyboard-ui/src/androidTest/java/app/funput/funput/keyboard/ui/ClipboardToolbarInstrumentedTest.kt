package app.funput.funput.keyboard.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.LocaleList
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.R
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.theme.KeyboardThemes
import java.time.Instant
import java.util.UUID
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardToolbarInstrumentedTest {
    @Test
    fun hintAndPasteCallbackAreForwardedWithoutExposingContent() = withKeyboard { keyboard ->
        var requests = 0
        keyboard.callbacks.onClipboardPasteRequested = { requests++ }
        keyboard.clipboardHint = KeyboardClipboardHint.SENSITIVE

        val surface = keyboard.surface()
        assertSame(KeyboardClipboardHint.SENSITIVE, surface.clipboardHint)
        surface.callbacks.onClipboardPasteRequested?.invoke()
        assertEquals(1, requests)
        keyboard.clipboardHint = null
        assertEquals(null, surface.clipboardHint)
    }

    @Test
    fun clipboardChipChangesToolbarPixelsAndVietnameseLabelsMatchIos() = withKeyboard { keyboard ->
        val surface = keyboard.surface()
        val before = surface.bitmap()
        keyboard.clipboardHint = KeyboardClipboardHint.TEXT
        val after = surface.bitmap()
        assertNotEquals(before.sameAs(after), true)

        val config = Configuration(keyboard.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag("vi")))
        }
        val resources = keyboard.context.createConfigurationContext(config).resources
        assertEquals("Dán", resources.getString(R.string.clipboard_paste))
        assertEquals("Đã sao chép văn bản", resources.getString(R.string.clipboard_hint_text))
        assertEquals("Đã sao chép một liên kết", resources.getString(R.string.clipboard_hint_link))
        assertEquals("Đã sao chép nội dung nhạy cảm", resources.getString(R.string.clipboard_hint_sensitive))
    }

    @Test
    fun clipboardChipRendersAcrossEveryPresetTheme() = withKeyboard { keyboard ->
        val themes = listOf(
            KeyboardThemes.Slate,
            KeyboardThemes.Ink,
            KeyboardThemes.Paper,
            KeyboardThemes.GlassDark,
            KeyboardThemes.GlassLight,
            KeyboardThemes.Blossom,
            KeyboardThemes.Orchid,
        )
        keyboard.clipboardHint = KeyboardClipboardHint.LINK
        themes.forEach { theme ->
            keyboard.keyboardTheme = theme
            assertEquals(keyboard.surface().width, keyboard.surface().bitmap().width)
        }
    }

    @Test
    fun clipboardPanelIsLazyReusedAndRendersEveryThemeAtNarrowWidth() = withKeyboard { keyboard ->
        var opened = 0
        keyboard.callbacks.onClipboardPanelOpened = { opened++ }
        keyboard.clipboardPanelEnabled = true
        keyboard.clipboardEntries = listOf(
            KeyboardClipboardEntry(UUID.randomUUID(), " Việt,\\\n 😀 ", Instant.now(), true),
        )
        assertTrue((0 until keyboard.childCount).none { keyboard.getChildAt(it) is ClipboardPanelView })
        keyboard.showClipboardPanel()
        val panel = (0 until keyboard.childCount).map(keyboard::getChildAt)
            .filterIsInstance<ClipboardPanelView>().single()
        assertEquals(KeyboardPanel.CLIPBOARD, keyboard.activePanel)
        assertEquals(1, opened)
        keyboard.showLettersPanel()
        keyboard.showClipboardPanel()
        assertSame(panel, (0 until keyboard.childCount).map(keyboard::getChildAt)
            .filterIsInstance<ClipboardPanelView>().single())
        assertEquals(2, opened)
        keyboard.measure(exactly(320), exactly(420))
        keyboard.layout(0, 0, 320, 420)
        listOf(
            KeyboardThemes.Slate, KeyboardThemes.Ink, KeyboardThemes.Paper,
            KeyboardThemes.GlassDark, KeyboardThemes.GlassLight,
            KeyboardThemes.Blossom, KeyboardThemes.Orchid,
        ).forEach { theme -> keyboard.keyboardTheme = theme; keyboard.bitmap() }
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

    private fun FunputKeyboardView.surface() = getChildAt(0) as KeyboardSurfaceView
    private fun KeyboardSurfaceView.bitmap() = Bitmap.createBitmap(
        width, height, Bitmap.Config.ARGB_8888,
    ).also { draw(Canvas(it)) }
    private fun FunputKeyboardView.bitmap() = Bitmap.createBitmap(
        width, height, Bitmap.Config.ARGB_8888,
    ).also { draw(Canvas(it)) }
    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
}

package app.funput.funput.keyboard.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.funput.funput.keyboard.ui.support.childOfType
import app.funput.funput.keyboard.ui.support.descendants
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.clipboard.row.ClipboardCardInset
import app.funput.funput.keyboard.ui.clipboard.row.ClipboardDeleteWidth
import app.funput.funput.theme.KeyboardThemes
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipboardSwipeInstrumentedTest {
    @Test
    fun swipeRevealsDeleteActionAndTapDeletes() {
        var removals = 0
        ActivityScenario.launch(EmojiTestActivity::class.java).use { scenario ->
            lateinit var panel: ClipboardPanelView
            lateinit var keyboard: FunputKeyboardView
            scenario.onActivity { activity ->
                keyboard = FunputKeyboardView(activity).apply {
                    keyboardTheme = KeyboardThemes.Slate
                    clipboardPanelEnabled = true
                    clipboardEntries = listOf(entry())
                    callbacks.onClipboardEntryRemoved = { removals++ }
                    showClipboardPanel()
                }
                activity.setContentView(keyboard)
                keyboard.measure(exactly(1080), exactly(600))
                keyboard.layout(0, 0, 1080, 600)
                panel = keyboard.descendants()
                    .filterIsInstance<ClipboardPanelView>().single()
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { swipeRowLeft(panel) }
            SystemClock.sleep(300)
            assertEquals(0, removals)
            scenario.onActivity {
                listOf(
                    KeyboardThemes.Slate, KeyboardThemes.Ink, KeyboardThemes.Paper,
                    KeyboardThemes.GlassDark, KeyboardThemes.GlassLight, KeyboardThemes.LiquidGlass,
                    KeyboardThemes.Blossom, KeyboardThemes.Orchid,
                ).forEach { theme -> keyboard.keyboardTheme = theme; render(keyboard) }
                swipeRowVertically(panel)
            }
            SystemClock.sleep(100)
            assertEquals(0, removals)
            scenario.onActivity { tapDelete(panel) }
            SystemClock.sleep(100)
            assertEquals(1, removals)
        }
    }

    private fun swipeRowLeft(view: View) {
        val density = view.resources.displayMetrics.density
        val start = cardRight(view) - 20f * density
        // Past half the delete action, which is where a swipe starts to reveal it.
        val end = start - (ClipboardDeleteWidth.value / 2f + 20f) * density
        gesture(view, start, cardY(view), end, cardY(view))
    }

    private fun tapDelete(view: View) {
        val x = cardRight(view) - ClipboardDeleteWidth.value / 2f * view.resources.displayMetrics.density
        gesture(view, x, cardY(view), x, cardY(view))
    }

    private fun swipeRowVertically(view: View) {
        val density = view.resources.displayMetrics.density
        gesture(view, view.width / 2f, cardY(view), view.width / 2f, cardY(view) + 76f * density)
    }

    private fun cardRight(view: View) =
        view.width - ClipboardCardInset.value * view.resources.displayMetrics.density

    /** Inside the first card: below the sticky section header and the gap under it. */
    private fun cardY(view: View) = 56f * view.resources.displayMetrics.density

    private fun gesture(view: View, startX: Float, startY: Float, endX: Float, endY: Float) {
        val downTime = SystemClock.uptimeMillis()
        listOf(
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0),
            MotionEvent.obtain(downTime, downTime + 16, MotionEvent.ACTION_MOVE, endX, endY, 0),
            MotionEvent.obtain(downTime, downTime + 32, MotionEvent.ACTION_UP, endX, endY, 0),
        ).forEach { event -> view.dispatchTouchEvent(event); event.recycle() }
    }

    private fun entry() = KeyboardClipboardEntry(UUID.randomUUID(), "Clipboard item", Instant.now(), false)
    private fun render(view: View) = Bitmap.createBitmap(
        view.width, view.height, Bitmap.Config.ARGB_8888,
    ).also { view.draw(Canvas(it)) }
    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
}

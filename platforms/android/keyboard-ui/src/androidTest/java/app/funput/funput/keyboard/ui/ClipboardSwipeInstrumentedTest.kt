package app.funput.funput.keyboard.ui

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.keyboard.ui.clipboard.ClipboardPanelView
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
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
            scenario.onActivity { activity ->
                val keyboard = FunputKeyboardView(activity).apply {
                    keyboardTheme = KeyboardThemes.Slate
                    clipboardPanelEnabled = true
                    clipboardEntries = listOf(entry())
                    callbacks.onClipboardEntryRemoved = { removals++ }
                    showClipboardPanel()
                }
                activity.setContentView(keyboard)
                keyboard.measure(exactly(1080), exactly(600))
                keyboard.layout(0, 0, 1080, 600)
                panel = (0 until keyboard.childCount).map(keyboard::getChildAt)
                    .filterIsInstance<ClipboardPanelView>().single()
            }
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { swipeRowLeft(panel) }
            SystemClock.sleep(300)
            assertEquals(0, removals)
            scenario.onActivity { tapDelete(panel) }
            SystemClock.sleep(100)
            assertEquals(1, removals)
        }
    }

    private fun swipeRowLeft(view: View) {
        val density = view.resources.displayMetrics.density
        gesture(view, view.width - 16f, 44f * density, view.width - 104f * density, 44f * density)
    }

    private fun tapDelete(view: View) {
        val density = view.resources.displayMetrics.density
        gesture(view, view.width - 44f * density, 44f * density, view.width - 44f * density, 44f * density)
    }

    private fun gesture(view: View, startX: Float, startY: Float, endX: Float, endY: Float) {
        val downTime = SystemClock.uptimeMillis()
        listOf(
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0),
            MotionEvent.obtain(downTime, downTime + 16, MotionEvent.ACTION_MOVE, endX, endY, 0),
            MotionEvent.obtain(downTime, downTime + 32, MotionEvent.ACTION_UP, endX, endY, 0),
        ).forEach { event -> view.dispatchTouchEvent(event); event.recycle() }
    }

    private fun entry() = KeyboardClipboardEntry(UUID.randomUUID(), "Clipboard item", Instant.now(), false)
    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
}

package app.funput.funput.keyboard.ui

import android.app.Activity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.keyboard.ui.emoji.EmojiPanelPalette
import app.funput.funput.keyboard.ui.emoji.EmojiSearchContentView
import app.funput.funput.keyboard.ui.emoji.EmojiSearchMode
import app.funput.funput.keyboard.ui.emoji.EmojiSearchState
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemes
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmojiPanelInstrumentedTest {
    @Test
    fun panelCanRethemeAcrossEveryPresetWhileOpen() {
        ActivityScenario.launch(EmojiTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val parent = LinearLayout(activity)
                val panel = EmojiPanelView(activity)
                parent.addView(panel, LinearLayout.LayoutParams(-1, -1))
                activity.setContentView(parent)
                LocalKeyboardThemeCatalog.themes
                    .map(KeyboardThemeDescriptor::theme)
                    .forEach(panel::updateTheme)
            }
        }
    }

    @Test
    fun localSearchKeyboardCanBeHostedInsideCompose() {
        ActivityScenario.launch(EmojiTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = FrameLayout(activity)
                KeyboardComposeLifecycle.install(root)
                val theme = KeyboardThemes.GlassDark
                val search = EmojiSearchContentView(activity).apply {
                    updateTheme(theme)
                    render(
                        EmojiSearchState(EmojiSearchMode.EDITING, "smile"),
                        emptyList(),
                        EmojiPanelPalette.from(theme),
                    )
                }
                root.addView(search, FrameLayout.LayoutParams(-1, -1))
                activity.setContentView(root)
            }
        }
    }
}

class EmojiTestActivity : Activity()

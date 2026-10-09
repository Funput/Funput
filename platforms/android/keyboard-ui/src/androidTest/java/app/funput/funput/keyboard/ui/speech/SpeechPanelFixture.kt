package app.funput.funput.keyboard.ui.speech

import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ActivityScenario
import app.funput.funput.keyboard.ui.EmojiTestActivity
import app.funput.funput.keyboard.ui.FunputKeyboardView
import kotlin.math.roundToInt
import java.util.Locale

internal class SpeechPanelFixture(
    fontScale: Float = 1f,
    widthDp: Int = 360,
    locale: Locale = Locale.forLanguageTag("vi"),
) : AutoCloseable {
    private val scenario = ActivityScenario.launch(EmojiTestActivity::class.java)
    lateinit var keyboard: FunputKeyboardView
        private set

    init {
        scenario.onActivity { activity ->
            val config = Configuration(activity.resources.configuration).apply {
                this.fontScale = fontScale
                setLocale(locale)
            }
            val context = activity.createConfigurationContext(config)
            keyboard = FunputKeyboardView(context)
            val density = context.resources.displayMetrics.density
            val width = (widthDp * density).roundToInt()
            val height = (280 * density).roundToInt()
            activity.setContentView(keyboard, android.view.ViewGroup.LayoutParams(width, height))
            keyboard.measure(exactly(width), exactly(height))
            keyboard.layout(0, 0, width, height)
        }
    }

    fun update(block: (FunputKeyboardView) -> Unit) { scenario.onActivity { block(keyboard) } }

    override fun close() { scenario.close() }

    private fun exactly(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
}

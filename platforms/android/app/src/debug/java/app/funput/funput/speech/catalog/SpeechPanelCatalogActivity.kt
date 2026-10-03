package app.funput.funput.speech.catalog

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.res.Configuration
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.speech.SpeechPanelAction
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import app.funput.funput.keyboard.ui.speech.SpeechPanelState
import app.funput.funput.keyboard.ui.speech.cancelSpeechPanel
import app.funput.funput.keyboard.utility.KeyboardMicrophoneState
import app.funput.funput.theme.LocalKeyboardThemeCatalog

/** Fake-state review surface. It never accesses a recognizer, permission or setup service. */
class SpeechPanelCatalogActivity : ComponentActivity() {
    private lateinit var keyboard: FunputKeyboardView
    private lateinit var host: LinearLayout
    private var stage = SpeechPanelStage.PREPARING
    private val themes = LocalKeyboardThemeCatalog.themes
    private var themeIndex = 0
    private var largeText = false
    private var english = false
    private var oneHanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stage = SpeechPanelStage.entries[intent.getIntExtra("speech_stage", 0).coerceIn(0, 3)]
        themeIndex = intent.getIntExtra("speech_theme", 0).coerceIn(themes.indices)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // The keyboard host owns navigation/caption insets; the catalog header owns status.
            view.setPadding(0, bars.top, 0, 0)
            insets
        }
        root.addView(TextView(this).apply {
            text = "Speech UI · State giả · Không thu âm\nBấm mic để mở panel; Back = Huỷ."
            setPadding(16, 16, 16, 16)
        })
        root.addView(selector(listOf("Preparing", "Listening", "Finalizing", "Error")) {
            stage = SpeechPanelStage.entries[it]
            if (::keyboard.isInitialized) showState()
        }.apply { setSelection(stage.ordinal) })
        root.addView(selector(themes.map { it.name }) {
            themeIndex = it
            if (::keyboard.isInitialized) keyboard.keyboardTheme = theme()
        }.apply { setSelection(themeIndex) })
        root.addView(selector(listOf("Tiếng Việt", "Tiếng Anh")) {
            english = it == 1
            if (::keyboard.isInitialized) updateMic()
        })
        root.addView(selector(listOf("Tiêu chuẩn", "Một tay")) {
            oneHanded = it == 1
            if (::keyboard.isInitialized) updatePlacement()
        })
        root.addView(CheckBox(this).apply {
            text = "Chữ 200% (chỉ trong catalog)"
            setOnCheckedChangeListener { _, checked -> largeText = checked; rebuild() }
        })
        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        host = LinearLayout(this)
        root.addView(host, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        rebuild()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!keyboard.cancelSpeechPanel()) finish()
            }
        })
    }

    private fun rebuild() {
        if (!::host.isInitialized) return
        host.removeAllViews()
        val config = Configuration(resources.configuration).apply { fontScale = if (largeText) 2f else 1f }
        keyboard = FunputKeyboardView(createConfigurationContext(config)).apply {
            keyboardTheme = theme()
            suggestions = listOf("xin", "chào", "bạn")
            hapticsEnabled = false
            soundsEnabled = false
            callbacks.onSpeechRequested = ::showState
            callbacks.onSpeechAction = { action ->
                when (action) {
                    SpeechPanelAction.STOP -> { stage = SpeechPanelStage.FINALIZING; showState() }
                    SpeechPanelAction.RETRY -> { stage = SpeechPanelStage.PREPARING; showState() }
                    SpeechPanelAction.OPEN_SETUP -> {
                        speechPanelState = speechPanelState.copy(message = "CTA mẫu; không mở thiết lập.")
                    }
                    SpeechPanelAction.CANCEL -> Unit
                }
            }
        }
        host.addView(keyboard, LinearLayout.LayoutParams(-1, -2))
        updateMic()
        updatePlacement()
        if (intent.getBooleanExtra("speech_show", false)) showState()
        if (intent.getBooleanExtra("speech_snapshot", false)) snapshotSpeechCatalog(keyboard)
    }

    private fun showState() {
        keyboard.speechPanelState = SpeechPanelState(stage, language(),
            preview = if (!intent.getBooleanExtra("speech_empty", false) &&
                stage in listOf(SpeechPanelStage.LISTENING, SpeechPanelStage.FINALIZING))
                "Mẫu: Hôm nay tôi thử nhập bằng giọng nói trong Funput." else "",
            message = if (stage == SpeechPanelStage.ERROR) "Thông báo lỗi mẫu: chưa có model." else null,
            canRetry = true, canOpenSetup = false)
        keyboard.showSpeechPanel()
    }

    private fun updateMic() {
        keyboard.language = language()
        keyboard.microphone = KeyboardMicrophoneState(visible = true,
            accessibilityLabel = "Nhập bằng giọng nói, ${if (english) "Tiếng Anh" else "Tiếng Việt"}")
        keyboard.speechPanelState = keyboard.speechPanelState.copy(language = language())
    }

    private fun updatePlacement() {
        keyboard.placementPreferences = KeyboardPlacementPreferences.Default.copy(
            activeMode = if (oneHanded) KeyboardPlacementMode.ONE_HANDED else KeyboardPlacementMode.STANDARD)
    }

    private fun language() = if (english) KeyboardLanguage.ENGLISH else KeyboardLanguage.VIETNAMESE
    private fun theme() = themes[themeIndex].theme

    private fun selector(labels: List<String>, selected: (Int) -> Unit) = Spinner(this).apply {
        adapter = ArrayAdapter(this@SpeechPanelCatalogActivity, android.R.layout.simple_spinner_dropdown_item, labels)
        onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selected(position)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }
}

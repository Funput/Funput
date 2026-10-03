package app.funput.funput.speech

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.text.InputType
import android.view.WindowManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.funput.funput.R

/** Synthetic editor host for P0: recognition still runs exclusively inside the real IME. */
class SpeechSpikeEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        content.addView(TextView(this).apply { setText(R.string.speech_spike_editor_explanation) })
        content.addView(Button(this).apply {
            setText(R.string.speech_spike_editor_setup)
            setOnClickListener { startActivity(Intent(context, SpeechSetupActivity::class.java)) }
        })
        val first = field(R.string.speech_spike_editor_first, InputType.TYPE_CLASS_TEXT)
        content.addView(first)
        content.addView(field(R.string.speech_spike_editor_second, InputType.TYPE_CLASS_TEXT))
        content.addView(field(R.string.speech_spike_editor_password,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        val scroll = ScrollView(this).apply { addView(content) }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(scroll)
        first.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun field(hintResource: Int, type: Int) = EditText(this).apply {
        setHint(hintResource)
        inputType = type
        isSaveEnabled = false // Test transcript stays only in the active editor's RAM.
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        if (Build.VERSION.SDK_INT >= 30) importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO
    }
}

package app.funput.funput.ime.speech.integration

import android.content.Context
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.funput.funput.ime.R
import app.funput.funput.ime.speech.session.SpeechPhase
import app.funput.funput.ime.speech.session.SpeechSessionState

/** Temporary P0 controls; the production renderer/panel is deliberately a later phase. */
@SuppressLint("ViewConstructor") // Created with explicit inert callbacks, never inflated from XML.
internal class SpeechSpikeHost(
    context: Context,
    private val keyboard: View,
    start: () -> Unit,
    stop: () -> Unit,
    cancel: () -> Unit,
    permission: () -> Unit,
    private val beforeKeyboardTouch: () -> Unit,
) : LinearLayout(context) {
    private val status = TextView(context).apply {
        text = context.getString(R.string.speech_spike_idle)
        maxLines = 3
        setPadding(12, 4, 12, 4)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    private val startButton = button(R.string.speech_spike_start, start)
    private val stopButton = button(R.string.speech_spike_stop, stop)
    private val cancelButton = button(R.string.speech_spike_cancel, cancel)

    init {
        orientation = VERTICAL
        setBackgroundColor(0xffeeeeee.toInt())
        status.setTextColor(0xff111111.toInt())
        addView(status, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(LinearLayout(context).apply {
            addView(startButton, controlParams())
            addView(stopButton, controlParams())
            addView(cancelButton, controlParams())
            addView(button(R.string.speech_spike_permission, permission), controlParams())
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(keyboard, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        render(SpeechSessionState(SpeechPhase.IDLE))
    }

    fun message(resource: Int) { status.setText(resource) }

    fun render(state: SpeechSessionState) {
        val message = context.getString(R.string.speech_spike_state,
            state.phase.name, state.error?.let { "${it.name} (${state.backendErrorCode ?: "local"})" }
                ?: context.getString(R.string.speech_spike_title))
        status.text = context.getString(R.string.speech_spike_preview, message, state.preview)
        startButton.isEnabled = state.phase == SpeechPhase.IDLE || state.phase == SpeechPhase.ERROR
        stopButton.isEnabled = state.phase == SpeechPhase.PREPARING || state.phase == SpeechPhase.LISTENING
        cancelButton.isEnabled = state.phase != SpeechPhase.IDLE
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && event.y >= keyboard.top) beforeKeyboardTouch()
        return super.dispatchTouchEvent(event)
    }

    private fun button(label: Int, action: () -> Unit) = Button(context).apply {
        setText(label)
        contentDescription = context.getString(label)
        isFocusable = false
        minHeight = (48 * resources.displayMetrics.density).toInt()
        setOnClickListener { action() }
    }

    private fun controlParams() = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
}

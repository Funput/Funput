package app.funput.funput.ime.speech.integration

import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import app.funput.funput.ime.speech.session.SpeechCancellation
import app.funput.funput.ime.speech.session.SpeechScheduler
import app.funput.funput.ime.speech.session.SpeechClock

internal class ImeSpeechMainScheduler : SpeechScheduler, SpeechClock {
    override fun nowMillis() = SystemClock.elapsedRealtime()
    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(delayMillis: Long, task: () -> Unit): SpeechCancellation {
        val runnable = Runnable(task)
        handler.postDelayed(runnable, delayMillis)
        return SpeechCancellation { handler.removeCallbacks(runnable) }
    }
}

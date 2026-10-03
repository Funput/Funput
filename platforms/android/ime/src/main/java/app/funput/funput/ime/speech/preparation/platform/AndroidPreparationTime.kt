package app.funput.funput.ime.speech.preparation.platform

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

internal class AndroidPreparationTime : PreparationTime {
    private val handler = Handler(Looper.getMainLooper())
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
    override fun schedule(delayMillis: Long, task: () -> Unit): PreparationCancellation {
        val runnable = Runnable(task)
        handler.postDelayed(runnable, delayMillis)
        return PreparationCancellation { handler.removeCallbacks(runnable) }
    }
}

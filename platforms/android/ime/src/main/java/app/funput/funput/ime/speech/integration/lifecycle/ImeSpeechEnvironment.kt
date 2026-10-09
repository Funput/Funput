package app.funput.funput.ime.speech.integration.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build

internal class ImeSpeechEnvironment(private val service: InputMethodService, screenOff: () -> Unit) {
    private var closed = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!closed) screenOff()
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) service.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else service.registerReceiver(receiver, filter)
    }

    fun close() {
        if (closed) return
        closed = true
        service.unregisterReceiver(receiver)
    }
}

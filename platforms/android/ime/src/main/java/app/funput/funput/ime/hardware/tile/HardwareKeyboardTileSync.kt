package app.funput.funput.ime.hardware.tile

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-process signal from [HardwareKeyboardTileService] to the active IME window.
 *
 * Some Android ROMs (HyperOS/MIUI, ColorOS) suppress `requestShowSelf()` while the Control Center
 * shade covers the screen; emitting again from `onStopListening()` lets the IME raise the soft
 * keyboard as soon as the shade closes.
 */
internal object HardwareKeyboardTileSync {
    private val mutableRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val requests: SharedFlow<Unit> = mutableRequests.asSharedFlow()

    fun requestSync() {
        mutableRequests.tryEmit(Unit)
    }
}

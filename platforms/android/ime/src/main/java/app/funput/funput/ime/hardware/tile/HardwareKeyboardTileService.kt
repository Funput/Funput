package app.funput.funput.ime.hardware.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.funput.funput.ime.R
import app.funput.funput.ime.settings.hardware.HardwareKeyboardPreferences
import app.funput.funput.ime.settings.hardware.HardwareKeyboardSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings / Control Center tile that toggles whether the on-screen Funput keyboard is shown
 * while a physical (Bluetooth or USB) keyboard is connected.
 */
class HardwareKeyboardTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settings by lazy { HardwareKeyboardSettings(this) }
    private var listeningJob: Job? = null
    private var currentState: HardwareKeyboardTileState? = null

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            settings.preferences.collect { preferences ->
                render(HardwareKeyboardTileState.from(preferences.showsSoftKeyboard))
            }
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        // HyperOS/MIUI and ColorOS hold window focus while the Control Center shade is open;
        // syncing again as the shade closes raises the soft keyboard in the focused editor.
        HardwareKeyboardTileSync.requestSync()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val cached = currentState
        if (cached != null) {
            val next = cached.toggled()
            render(next)
            serviceScope.launch {
                settings.setShowsSoftKeyboard(next.showsSoftKeyboard)
                HardwareKeyboardTileSync.requestSync()
            }
        } else {
            serviceScope.launch {
                val current = settings.preferences.first()
                val next = HardwareKeyboardTileState.from(!current.showsSoftKeyboard)
                render(next)
                settings.setShowsSoftKeyboard(next.showsSoftKeyboard)
                HardwareKeyboardTileSync.requestSync()
            }
        }
    }

    override fun onDestroy() {
        listeningJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun render(state: HardwareKeyboardTileState) {
        currentState = state
        val tile: Tile = qsTile ?: return
        tile.state = state.tileState
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_hardware_keyboard)
        tile.label = getString(state.labelRes)
        tile.contentDescription = getString(state.contentDescriptionRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(state.subtitleRes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = getString(state.subtitleRes)
        }
        tile.updateTile()
    }
}

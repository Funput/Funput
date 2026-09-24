package app.funput.funput.ui.settings.hardware

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.settings.hardware.HardwareKeyboardPreferences
import app.funput.funput.ime.settings.hardware.HardwareKeyboardSettings
import app.funput.funput.ui.keyboard.openHardwareKeyboardSettings
import kotlinx.coroutines.launch

/** What the hardware-keyboard section renders and every action it can dispatch. */
@Immutable
internal class HardwareKeyboardSectionState(
    val preferences: HardwareKeyboardPreferences,
    val onShowsSoftKeyboardChanged: (Boolean) -> Unit,
    val onToggleHotkeyChanged: (Boolean) -> Unit,
    val onOpenSystemSettings: () -> Unit,
) {
    companion object {
        /** Defaults with no-op actions, for previews and tests that do not exercise this section. */
        val Inert = HardwareKeyboardSectionState(
            preferences = HardwareKeyboardPreferences.Default,
            onShowsSoftKeyboardChanged = {},
            onToggleHotkeyChanged = {},
            onOpenSystemSettings = {},
        )
    }
}

/**
 * Binds the section to its own store.
 *
 * `FunputSettingsState` already carries every other setting and sits near the per-file line
 * limit; a section that only reads and writes one store does not need to go through it.
 */
@Composable
internal fun rememberHardwareKeyboardSectionState(): HardwareKeyboardSectionState {
    val context = LocalContext.current
    val store = remember(context) { HardwareKeyboardSettings(context) }
    val scope = rememberCoroutineScope()
    val preferences by store.preferences.collectAsState(HardwareKeyboardPreferences.Default)
    return HardwareKeyboardSectionState(
        preferences = preferences,
        onShowsSoftKeyboardChanged = { enabled -> scope.launch { store.setShowsSoftKeyboard(enabled) } },
        onToggleHotkeyChanged = { enabled -> scope.launch { store.setToggleHotkeyEnabled(enabled) } },
        onOpenSystemSettings = context::openHardwareKeyboardSettings,
    )
}

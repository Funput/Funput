package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsScreenState,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<SettingsPicker?>(null) }
    val scrollBehavior = rememberSettingsScrollBehavior()
    Scaffold(
        topBar = { SettingsTopBar(scrollBehavior) },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        SettingsScreenSections(
            state = state,
            contentPadding = contentPadding,
            onOpenPicker = { picker = it },
        )
    }
    SettingsPickerSheet(
        picker = picker,
        inputMethod = state.inputMethod,
        toneStyle = state.toneStyle,
        keyboardPlacementMode = state.placement.activeMode,
        clipboardExpiry = state.clipboardPreferences.expiry,
        onInputMethodSelected = state.onInputMethodSelected,
        onToneStyleSelected = state.onToneStyleSelected,
        onKeyboardPlacementSelected = state.onPlacementModeSelected,
        onClipboardExpirySelected = state.onClipboardExpirySelected,
        onDismiss = { picker = null },
    )
}

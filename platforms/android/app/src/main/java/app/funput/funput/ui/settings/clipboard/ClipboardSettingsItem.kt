package app.funput.funput.ui.settings.clipboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ui.theme.EntryTracker
import app.funput.funput.ui.theme.staggeredEntry

internal fun LazyListScope.clipboardSettingsItem(
    preferences: ClipboardPreferences,
    tracker: EntryTracker,
    onEnabledChanged: (Boolean) -> Unit,
    onOpenExpiry: () -> Unit,
    onClear: () -> Unit,
) {
    item(key = "clipboard") {
        Box(modifier = Modifier.staggeredEntry(4, tracker)) {
            ClipboardSettingsSection(
                enabled = preferences.enabled,
                expiry = preferences.expiry,
                onEnabledChanged = onEnabledChanged,
                onOpenExpiry = onOpenExpiry,
                onClear = onClear,
            )
        }
    }
}

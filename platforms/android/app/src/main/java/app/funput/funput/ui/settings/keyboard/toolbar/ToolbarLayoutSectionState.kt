package app.funput.funput.ui.settings.keyboard.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.settings.layout.ToolbarLayoutPreferences
import app.funput.funput.ime.settings.layout.ToolbarLayoutSettings
import kotlinx.coroutines.launch

/** What the toolbar-layout section shows and the edits it can apply. */
@Immutable
internal class ToolbarLayoutSectionState(
    val preferences: ToolbarLayoutPreferences,
    val onMoveUp: (ToolbarSide, Int) -> Unit,
    val onMoveDown: (ToolbarSide, Int) -> Unit,
    val onMoveToOtherSide: (ToolbarSide, Int) -> Unit,
) {
    companion object {
        val Inert = ToolbarLayoutSectionState(
            preferences = ToolbarLayoutPreferences.Default,
            onMoveUp = { _, _ -> },
            onMoveDown = { _, _ -> },
            onMoveToOtherSide = { _, _ -> },
        )
    }
}

/** Binds the section to its own store so [app.funput.funput.ui.FunputSettingsState] stays lean. */
@Composable
internal fun rememberToolbarLayoutSectionState(): ToolbarLayoutSectionState {
    val context = LocalContext.current
    val store = remember(context) { ToolbarLayoutSettings(context) }
    val scope = rememberCoroutineScope()
    val preferences by store.preferences.collectAsState(ToolbarLayoutPreferences.Default)
    fun write(transform: (ToolbarLayoutPreferences) -> ToolbarLayoutPreferences) {
        scope.launch { store.updatePreferences(transform) }
    }
    return ToolbarLayoutSectionState(
        preferences = preferences,
        onMoveUp = { side, index -> write { ToolbarLayoutEdits.moveUp(it, side, index) } },
        onMoveDown = { side, index -> write { ToolbarLayoutEdits.moveDown(it, side, index) } },
        onMoveToOtherSide = { side, index ->
            write { ToolbarLayoutEdits.moveToOtherSide(it, side, index) }
        },
    )
}

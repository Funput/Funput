package app.funput.funput.ui.settings.quick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.kit.cards.FunputSectionHeader
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tiles.FunputToggleTile
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.label

/**
 * The top of the keyboard hub: the input method as one segmented control, then a grid of
 * [QuickSetting] tiles. Three columns, two once the text is large enough to need the room.
 */
internal fun LazyListScope.quickSettingsItems(state: SettingsScreenState) {
    item(key = "quick-input-method") {
        Column(Modifier.testTag(QuickInputMethodTag)) {
            FunputSectionHeader(stringResource(R.string.quick_input_method_header))
            FunputSegmented(
                options = KeyboardInputMethod.entries.map { it.label() },
                selectedIndex = state.inputMethod.ordinal,
                onSelect = { index -> state.onInputMethodSelected(KeyboardInputMethod.entries[index]) },
            )
        }
    }
    item(key = "quick-settings") {
        Column {
            FunputSectionHeader(stringResource(R.string.quick_settings_header))
            QuickSettingsGrid(state)
        }
    }
}

@Composable
private fun QuickSettingsGrid(state: SettingsScreenState) {
    val columns = if (LocalDensity.current.fontScale >= LargeTextScale) 2 else 3
    val gap = FunputUi.spacing.small
    // Plain rows rather than a lazy grid: six tiles, inside a list that already scrolls.
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        QuickSetting.entries.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                row.forEach { setting ->
                    FunputToggleTile(
                        title = stringResource(setting.title),
                        state = stringResource(setting.stateLabel(state)),
                        icon = setting.icon,
                        checked = setting.isOn(state),
                        onCheckedChange = { setting.toggle(state) },
                        accessibilityTitle = stringResource(setting.accessibilityTitle),
                        locked = setting.isLocked(state),
                        lockedReason = stringResource(R.string.quick_vni_number_row),
                        modifier = Modifier.weight(1f).testTag(quickSettingTag(setting)),
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** From this font scale on, three tiles side by side no longer fit their names. */
private const val LargeTextScale = 1.3f

/** Test tag of the input method control. */
internal const val QuickInputMethodTag = "quick-input-method"

/** Test tag of the tile for [setting]. */
internal fun quickSettingTag(setting: QuickSetting): String = "quick-${setting.name.lowercase()}"

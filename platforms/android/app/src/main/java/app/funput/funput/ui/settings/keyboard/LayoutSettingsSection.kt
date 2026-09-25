package app.funput.funput.ui.settings.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.KeyboardPlacementResolver
import app.funput.funput.keyboard.placement.OneHandedSide
import app.funput.funput.ui.settings.components.SettingsPercentSliderRow
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSegmentedRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.settings.label

@Composable
internal fun LayoutSettingsSection(
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    keySizeProfile: KeyboardSizingProfile,
    placement: KeyboardPlacementPreferences = KeyboardPlacementPreferences.Default,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onOpenPlacement: () -> Unit = {},
    onElevationSelected: (Float) -> Unit = {},
    onOneHandedWidthSelected: (Float) -> Unit = {},
    onOneHandedSideSelected: (OneHandedSide) -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val baseHeight = KeyboardDimensions.recommendedHeightDp(
        inputMethod, KeyboardEditorMode.TEXT, keySizeProfile,
        configuration.screenWidthDp.toFloat(), showsNumberRow,
    )
    val maximumOffset = KeyboardPlacementResolver.resolve(
        preferences = placement.copy(
            activeMode = KeyboardPlacementMode.ELEVATED,
            elevatedOffsetDp = configuration.screenHeightDp.toFloat(),
        ),
        density = 1f,
        viewportHeightPx = configuration.screenHeightDp,
        baseKeyboardHeightPx = baseHeight.toInt(),
    ).maximumOffsetPx.toFloat()
    SettingsSection(
        title = stringResource(R.string.settings_section_layout),
        rows = buildList {
            add { position ->
                SettingsRow(
                    position = position,
                    title = stringResource(R.string.settings_keyboard_mode_title),
                    value = placement.activeMode.label(),
                    iconRes = R.drawable.ic_keyboard,
                    onClick = onOpenPlacement,
                )
            }
            if (placement.activeMode == KeyboardPlacementMode.ELEVATED) {
                add { position ->
                    ElevationSliderRow(
                        position, placement.elevatedOffsetDp, maximumOffset, onElevationSelected,
                    )
                }
            }
            if (placement.activeMode == KeyboardPlacementMode.ONE_HANDED) {
                add { position ->
                    SettingsPercentSliderRow(
                        position = position,
                        title = stringResource(R.string.settings_one_handed_width_title),
                        iconRes = R.drawable.ic_key_size,
                        value = placement.oneHandedWidthFraction,
                        range = KeyboardPlacementPreferences.MinOneHandedWidthFraction..
                            KeyboardPlacementPreferences.MaxOneHandedWidthFraction,
                        onValueSettled = onOneHandedWidthSelected,
                    )
                }
                add { position ->
                    SettingsSegmentedRow(
                        position = position,
                        title = stringResource(R.string.settings_one_handed_side_title),
                        iconRes = R.drawable.ic_keyboard,
                        options = OneHandedSide.entries,
                        selected = placement.oneHandedSide,
                        labelOf = { it.label() },
                        onSelected = onOneHandedSideSelected,
                    )
                }
            }
            if (inputMethod.isTelexFamily) {
                add { position ->
                    SettingsSwitchRow(
                        position = position,
                        title = stringResource(R.string.settings_number_row_title),
                        summary = stringResource(R.string.settings_number_row_summary),
                        checked = showsNumberRow,
                        iconRes = R.drawable.ic_keyboard,
                        onCheckedChange = onShowsNumberRowChanged,
                    )
                }
            }
            add { position ->
                SettingsPercentSliderRow(
                    position = position,
                    title = stringResource(R.string.settings_key_size_title),
                    iconRes = R.drawable.ic_key_size,
                    value = keySizeProfile.heightScale,
                    range = KeyboardSizingProfile.MinScale..KeyboardSizingProfile.MaxScale,
                    onValueSettled = { scale ->
                        onKeySizeSelected(KeyboardSizingProfile.scaled(scale))
                    },
                )
            }
        },
        modifier = Modifier.testTag(LayoutSettingsSectionTag),
    )
}

internal const val LayoutSettingsSectionTag = "settings-section-layout"

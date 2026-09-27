package app.funput.funput.ui.settings.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.OneHandedSide
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.SegmentedRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.label

/**
 * The keyboard's shape and place: placement mode (with its own controls when elevated or
 * one-handed), the number row where the input method leaves it optional, and key size.
 */
@Composable
internal fun LayoutSection(
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    keySizeProfile: KeyboardSizingProfile,
    placement: KeyboardPlacementPreferences,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onOpenPlacement: () -> Unit,
    onElevationSelected: (Float) -> Unit,
    onOneHandedWidthSelected: (Float) -> Unit,
    onOneHandedSideSelected: (OneHandedSide) -> Unit,
) {
    val divider = @Composable { FunputDivider(startInset = FunputRowDefaults.IconDividerInset) }
    FunputSection(
        title = stringResource(R.string.settings_section_layout),
        modifier = Modifier.testTag(LayoutSettingsSectionTag),
    ) {
        LinkRow(
            title = stringResource(R.string.settings_keyboard_mode_title),
            value = placement.activeMode.label(),
            icon = FunputIcons.Placement,
            tint = FunputTint.BLUE,
            onClick = onOpenPlacement,
        )
        when (placement.activeMode) {
            KeyboardPlacementMode.ELEVATED -> {
                divider()
                val maximum = elevationMaximumDp(inputMethod, keySizeProfile, showsNumberRow, placement)
                ElevationSliderRow(placement.elevatedOffsetDp, maximum, onElevationSelected)
            }
            KeyboardPlacementMode.ONE_HANDED -> {
                divider()
                PercentSliderRow(
                    title = stringResource(R.string.settings_one_handed_width_title),
                    icon = FunputIcons.OneHanded,
                    value = placement.oneHandedWidthFraction,
                    range = KeyboardPlacementPreferences.MinOneHandedWidthFraction..
                        KeyboardPlacementPreferences.MaxOneHandedWidthFraction,
                    onSettled = onOneHandedWidthSelected,
                )
                divider()
                SegmentedRow(
                    title = stringResource(R.string.settings_one_handed_side_title),
                    options = OneHandedSide.entries.map { it.label() },
                    selectedIndex = placement.oneHandedSide.ordinal,
                    onSelect = { onOneHandedSideSelected(OneHandedSide.entries[it]) },
                    icon = FunputIcons.OneHanded,
                    tint = FunputTint.BLUE,
                )
            }
            KeyboardPlacementMode.STANDARD -> Unit
        }
        // VNI types tones with the digits, so for it the row is always on: nothing to decide.
        if (inputMethod.isTelexFamily) {
            divider()
            ToggleRow(
                title = stringResource(R.string.settings_number_row_title),
                summary = stringResource(R.string.settings_number_row_summary),
                checked = showsNumberRow,
                onCheckedChange = onShowsNumberRowChanged,
                icon = FunputIcons.NumberRow,
                tint = FunputTint.BLUE,
            )
        }
        divider()
        PercentSliderRow(
            title = stringResource(R.string.settings_key_size_title),
            icon = FunputIcons.KeySize,
            value = keySizeProfile.heightScale,
            range = KeyboardSizingProfile.MinScale..KeyboardSizingProfile.MaxScale,
            onSettled = { scale -> onKeySizeSelected(KeyboardSizingProfile.scaled(scale)) },
            showsRange = true,
        )
    }
}

/** Test tag of the layout section. */
internal const val LayoutSettingsSectionTag = "settings-section-layout"

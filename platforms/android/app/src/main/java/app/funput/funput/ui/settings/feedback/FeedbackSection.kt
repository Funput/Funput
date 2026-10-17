package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.sectionTitle

/**
 * What a key press feels and sounds like: how strongly it vibrates, chosen in a sheet, and key
 * sounds. One pink group.
 */
@Composable
internal fun FeedbackSection(
    hapticStrength: KeyboardHapticStrength?,
    soundsEnabled: Boolean,
    onOpenHapticStrength: () -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
) {
    FunputSection(
        title = sectionTitle(R.string.settings_section_feedback),
        modifier = Modifier.testTag(FeedbackSettingsSectionTag),
    ) {
        LinkRow(
            title = stringResource(R.string.settings_haptics_title),
            onClick = onOpenHapticStrength,
            summary = stringResource(R.string.settings_haptics_strength_summary),
            value = stringResource(HapticStrengthOption.of(hapticStrength).title),
            icon = FunputIcons.Haptics,
            tint = FunputTint.PINK,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        ToggleRow(
            title = stringResource(R.string.settings_sounds_title),
            checked = soundsEnabled,
            onCheckedChange = onSoundsChanged,
            icon = FunputIcons.Sound,
            tint = FunputTint.PINK,
        )
    }
}

/** Test tag of the feedback section. */
internal const val FeedbackSettingsSectionTag = "settings-section-feedback"

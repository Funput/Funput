package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.feedback.sound.KeySoundOption
import app.funput.funput.ui.settings.sectionTitle

/**
 * What a key press feels, sounds and looks like: how strongly it vibrates and how it sounds, each
 * chosen in a sheet, and the enlarged preview over the key. One pink group.
 */
@Composable
internal fun FeedbackSection(
    hapticStrength: KeyboardHapticStrength?,
    soundStyle: KeyboardSoundStyle?,
    keyPreviewsEnabled: Boolean,
    onOpenHapticStrength: () -> Unit,
    onOpenKeySound: () -> Unit,
    onKeyPreviewsChanged: (Boolean) -> Unit,
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
        LinkRow(
            title = stringResource(R.string.settings_sounds_title),
            onClick = onOpenKeySound,
            summary = stringResource(R.string.settings_sounds_style_summary),
            value = stringResource(KeySoundOption.of(soundStyle).title),
            icon = FunputIcons.Sound,
            tint = FunputTint.PINK,
            modifier = Modifier.testTag(FeedbackSoundRowTag),
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        ToggleRow(
            title = stringResource(R.string.settings_key_previews_title),
            summary = stringResource(R.string.settings_key_previews_summary),
            checked = keyPreviewsEnabled,
            onCheckedChange = onKeyPreviewsChanged,
            icon = FunputIcons.KeyPreview,
            tint = FunputTint.PINK,
        )
    }
}

/** Test tag of the feedback section. */
internal const val FeedbackSettingsSectionTag = "settings-section-feedback"

/** Test tag of the row opening the key sound sheet. */
internal const val FeedbackSoundRowTag = "settings-feedback-sound"

package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint

/** What a key press feels and sounds like: vibration and key sounds, one pink group. */
@Composable
internal fun FeedbackSection(
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
) {
    FunputSection(
        title = stringResource(R.string.settings_section_feedback),
        modifier = Modifier.testTag(FeedbackSettingsSectionTag),
    ) {
        ToggleRow(
            title = stringResource(R.string.settings_haptics_title),
            checked = hapticsEnabled,
            onCheckedChange = onHapticsChanged,
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

package app.funput.funput.ui.settings.feedback.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardSounds
import app.funput.funput.keyboard.sound.KeyboardSoundChoice
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.keyboard.sound.KeyboardSoundVolume
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.cards.FunputSectionHeader
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.ChoiceRow
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Choosing what key presses sound like and how loud. Like the vibration sheet it stays open, and
 * each choice plays once as it is made, so the user can hear them in turn. The volume sits on top,
 * where it is seen without scrolling, and is dimmed rather than removed while sound is off, so
 * turning sound off does not slide the list under the user's finger.
 */
@Composable
internal fun KeySoundSheet(
    selected: KeyboardSoundStyle?,
    volume: KeyboardSoundVolume,
    onSelect: (KeyboardSoundStyle?) -> Unit,
    onVolumeSelect: (KeyboardSoundVolume) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    FunputSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_sounds_title)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.large),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            Column {
                FunputSectionHeader(stringResource(R.string.key_sound_volume_header))
                FunputSegmented(
                    options = KeyboardSoundVolume.entries.map { stringResource(volumeTitle(it)) },
                    selectedIndex = volume.ordinal,
                    onSelect = { index ->
                        val chosen = KeyboardSoundVolume.entries[index]
                        onVolumeSelect(chosen)
                        selected?.let { KeyboardSounds.performSample(view, KeyboardSoundChoice(it, chosen)) }
                    },
                    enabled = selected != null,
                    modifier = Modifier.testTag(KeySoundVolumeTag),
                )
            }
            FunputSection(title = null, footer = stringResource(R.string.key_sound_hint)) {
                KeySoundOption.entries.forEachIndexed { index, option ->
                    if (index > 0) FunputDivider()
                    ChoiceRow(
                        title = stringResource(option.title),
                        summary = stringResource(option.summary),
                        selected = option.style == selected,
                        onSelect = {
                            onSelect(option.style)
                            option.style?.let { KeyboardSounds.performSample(view, KeyboardSoundChoice(it, volume)) }
                        },
                        modifier = Modifier.testTag(keySoundTag(option)),
                    )
                }
            }
        }
    }
}

/** Test tag of the volume control. */
internal const val KeySoundVolumeTag = "key-sound-volume"

/** Test tag of the row for [option]. */
internal fun keySoundTag(option: KeySoundOption): String = "key-sound-${option.name.lowercase()}"

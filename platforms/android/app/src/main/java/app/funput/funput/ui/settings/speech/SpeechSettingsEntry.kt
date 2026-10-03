package app.funput.funput.ui.settings.speech

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.LinkRow

@Composable
internal fun SpeechSettingsEntry(onOpen: () -> Unit) {
    FunputSection(title = null) {
        LinkRow(stringResource(R.string.speech_settings_title), onOpen,
            summary = stringResource(R.string.speech_settings_summary),
            modifier = Modifier.testTag("settings-speech"))
    }
}

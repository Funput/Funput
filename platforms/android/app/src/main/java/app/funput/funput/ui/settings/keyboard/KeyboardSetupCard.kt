package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

@Composable
internal fun KeyboardSetupCard(
    status: KeyboardSetupStatus,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nothing at all once setup is done. What this said then was that there was nothing to do,
    // which is a banner congratulating the user in perpetuity for something they did once.
    if (status == KeyboardSetupStatus.READY) return
    SetupJourneyCard(status, onEnableKeyboard, onSelectKeyboard, modifier)
}

@Composable
private fun SetupJourneyCard(
    status: KeyboardSetupStatus,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabling = status == KeyboardSetupStatus.NOT_ENABLED
    WatermarkedCard(modifier = modifier.heroCard()) {
      Column {
        SetupHeroHeader(
            iconRes = R.drawable.ic_keyboard,
            title = stringResource(R.string.settings_keyboard_setup_heading),
            subtitle = stringResource(R.string.settings_keyboard_setup_progress, if (enabling) 1 else 2),
        )
        Spacer(Modifier.height(18.dp))
        SetupStepRow(
            index = 1,
            state = if (enabling) StepState.ACTIVE else StepState.DONE,
            title = stringResource(R.string.settings_keyboard_setup_step_enable),
            connected = true,
        )
        SetupStepRow(
            index = 2,
            state = if (enabling) StepState.UPCOMING else StepState.ACTIVE,
            title = stringResource(R.string.settings_keyboard_setup_step_select),
            connected = false,
        )
        Spacer(Modifier.height(18.dp))
        SetupPrimaryButton(
            label = stringResource(
                if (enabling) R.string.settings_keyboard_setup_enable_action
                else R.string.settings_keyboard_setup_select_action,
            ),
            onClick = if (enabling) onEnableKeyboard else onSelectKeyboard,
        )
    }
  }
}


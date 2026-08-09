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
    if (status == KeyboardSetupStatus.READY) {
        SetupReadyCard(modifier)
    } else {
        SetupJourneyCard(status, onEnableKeyboard, onSelectKeyboard, modifier)
    }
}

@Composable
private fun SetupJourneyCard(
    status: KeyboardSetupStatus,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabling = status == KeyboardSetupStatus.NOT_ENABLED
    Column(modifier = modifier.heroCard()) {
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

@Composable
private fun SetupReadyCard(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.heroCard(),
    ) {
        SetupStepBadge(state = StepState.DONE, index = 0)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.settings_keyboard_setup_ready),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_keyboard_setup_ready_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

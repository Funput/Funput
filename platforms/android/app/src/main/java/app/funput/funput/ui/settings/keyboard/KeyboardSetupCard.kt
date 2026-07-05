package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    when (status) {
        KeyboardSetupStatus.READY -> ReadyCard(modifier)
        KeyboardSetupStatus.NOT_ENABLED -> SetupActionCard(
            title = stringResource(R.string.settings_keyboard_setup_not_enabled_title),
            body = stringResource(R.string.settings_keyboard_setup_not_enabled_body),
            actionLabel = stringResource(R.string.settings_keyboard_setup_enable_action),
            onAction = onEnableKeyboard,
            modifier = modifier,
        )
        KeyboardSetupStatus.NOT_SELECTED -> SetupActionCard(
            title = stringResource(R.string.settings_keyboard_setup_not_selected_title),
            body = stringResource(R.string.settings_keyboard_setup_not_selected_body),
            actionLabel = stringResource(R.string.settings_keyboard_setup_select_action),
            onAction = onSelectKeyboard,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReadyCard(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(R.string.settings_keyboard_setup_ready),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun SetupActionCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
            )
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(text = actionLabel)
        }
    }
}

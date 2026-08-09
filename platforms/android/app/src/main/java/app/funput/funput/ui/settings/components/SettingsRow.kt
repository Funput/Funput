package app.funput.funput.ui.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun SettingsRow(
    position: RowPosition,
    title: String,
    @DrawableRes iconRes: Int,
    tone: SettingsIconTone,
    onClick: () -> Unit,
    value: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    SettingsRowSurface(
        position = position,
        interactionSource = interactionSource,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        SettingsIcon(iconRes, tone)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        value?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun SettingsIcon(
    @DrawableRes iconRes: Int,
    tone: SettingsIconTone,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(MaterialTheme.shapes.small)
            .background(tone.container),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tone.content,
            modifier = Modifier.size(20.dp),
        )
    }
}

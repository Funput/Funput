package app.funput.funput.ui.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.theme.Spacing

/**
 * A row that leaves the app.
 *
 * The trailing mark is an outward arrow rather than the chevron the other rows carry, because the
 * two promise different things: a chevron says another screen of this app, an outward arrow says a
 * browser or a mail app is about to take over.
 */
@Composable
internal fun SettingsLinkRow(
    position: RowPosition,
    title: String,
    summary: String,
    @DrawableRes iconRes: Int,
    tone: SettingsIconTone,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hint = stringResource(R.string.about_external_link)
    SettingsRowSurface(
        position = position,
        interactionSource = interactionSource,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { onClick(label = hint, action = null) },
    ) {
        SettingsIcon(iconRes, tone)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_outward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp),
        )
    }
}

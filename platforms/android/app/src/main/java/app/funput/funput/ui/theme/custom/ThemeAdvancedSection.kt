package app.funput.funput.ui.theme.custom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/**
 * The twenty individual colour tokens, the metrics and the background image, behind one heading.
 *
 * They were the whole screen. Most people want a theme, not twenty decisions about renderer
 * tokens, and the two choices above this now produce a readable theme on their own — so this is
 * where you go when you want to chase a detail, not where you start.
 */
@Composable
internal fun ThemeAdvancedSection(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "advanced-chevron")
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onExpandedChange(!expanded) }
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.custom_theme_advanced_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}

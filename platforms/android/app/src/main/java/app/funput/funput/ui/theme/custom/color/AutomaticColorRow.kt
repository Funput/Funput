package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme

/**
 * A colour that is following another, shown as what it is rather than as a swatch to fill in.
 *
 * The point is to say "this one is handled" without hiding it: tapping still opens the picker,
 * which is what sets it apart from its source.
 */
@Composable
internal fun AutomaticColorRow(
    role: ThemeColorRole,
    theme: KeyboardTheme,
    onEdit: () -> Unit,
) {
    val source = role.follows ?: return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(vertical = 6.dp, horizontal = 8.dp),
    ) {
        Text(
            text = stringResource(role.labelRes),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.custom_theme_color_automatic, stringResource(source.labelRes)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

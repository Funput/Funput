package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme

@Composable
internal fun ThemeColorTab(
    theme: KeyboardTheme,
    onColorChange: (ThemeColorRole, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ThemeColorRole?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier) {
        ThemeColorGroup.entries.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(group.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                )
                ThemeColorRole.entries
                    .filter { role -> role.group == group }
                    .forEach { role ->
                        ColorRoleRow(
                            label = stringResource(role.labelRes),
                            color = role.read(theme),
                            onClick = { editing = role },
                        )
                    }
            }
        }
    }

    editing?.let { role ->
        val label = stringResource(role.labelRes)
        ColorPickerDialog(
            title = label,
            initialColor = role.read(theme),
            onDismiss = { editing = null },
            onConfirm = { color ->
                onColorChange(role, color)
                editing = null
            },
        )
    }
}

@Composable
private fun ColorRoleRow(label: String, color: Int, onClick: () -> Unit) {
    val description = stringResource(R.string.custom_theme_color_edit_description, label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Swatch(color)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Swatch(color: Int) {
    // A transparent token would otherwise be an invisible row, so the border marks the area.
    Column(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(color))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
    ) {}
}

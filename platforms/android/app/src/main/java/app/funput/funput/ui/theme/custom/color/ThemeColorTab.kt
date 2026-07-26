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
                        ColorSwatchRow(
                            label = stringResource(role.labelRes),
                            color = role.read(theme),
                            onClick = { editing = role },
                        )
                    }
            }
        }
        ThemeContrastWarnings(theme)
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

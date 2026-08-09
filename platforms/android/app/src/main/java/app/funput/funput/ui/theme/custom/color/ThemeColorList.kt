package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.ui.theme.custom.ThemeEditorTab

/**
 * The colours belonging to one editor page.
 *
 * Roles that still follow another are listed as following rather than as a swatch to fill in, and
 * the detail roles wait behind a disclosure — a page should open on the colours somebody came for.
 */
@Composable
internal fun ThemeColorList(
    tab: ThemeEditorTab,
    theme: KeyboardTheme,
    onColorChange: (ThemeColorRole, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ThemeColorRole?>(null) }
    var showsDetail by remember { mutableStateOf(false) }
    val roles = ThemeColorRole.entries.filter { role -> role.tab == tab }
    val detail = roles.filter { role -> role.group == ThemeColorGroup.Advanced }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        ThemeColorGroup.entries.forEach { group ->
            if (group == ThemeColorGroup.Advanced && !showsDetail) return@forEach
            val inGroup = roles.filter { role -> role.group == group }
            if (inGroup.isEmpty()) return@forEach
            val direct = inGroup.filter { role -> !ThemeColorLinks.isAutomatic(role, theme) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(group.titleRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                ColorRoleGrid(roles = direct, theme = theme, onSelect = { role -> editing = role })
                (inGroup - direct.toSet()).forEach { role ->
                    AutomaticColorRow(role, theme) { editing = role }
                }
            }
        }
        if (detail.isNotEmpty()) {
            TextButton(onClick = { showsDetail = !showsDetail }) {
                Text(
                    stringResource(
                        if (showsDetail) {
                            R.string.custom_theme_color_hide_advanced
                        } else {
                            R.string.custom_theme_color_show_advanced
                        },
                    ),
                )
            }
        }
    }

    ThemeColorTabDialogHost(
        editing = editing,
        theme = theme,
        onColorChange = onColorChange,
        onDismiss = { editing = null },
    )
}

package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.theme.custom.studio.ThemeEditorTab
import app.funput.funput.ui.theme.custom.color.picker.ColorPickerSheet

/**
 * The colours belonging to one editor page, one card per group.
 *
 * A role still following another says so ("Automatic · follows Accent") instead of asking to be
 * filled in, and the detail group waits behind a button: a page should open on the colours
 * somebody came for.
 */
@Composable
internal fun ThemeColorList(
    tab: ThemeEditorTab,
    theme: KeyboardTheme,
    onColorChange: (ThemeColorRole, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<ThemeColorRole?>(null) }
    var showsDetail by rememberSaveable { mutableStateOf(false) }
    val roles = ThemeColorRole.entries.filter { role -> role.tab == tab }
    val hasDetail = roles.any { role -> role.group == ThemeColorGroup.Advanced }
    Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.section), modifier = modifier) {
        ThemeColorGroup.entries.forEach { group ->
            val inGroup = roles.filter { role -> role.group == group }
            if (inGroup.isEmpty() || (group == ThemeColorGroup.Advanced && !showsDetail)) return@forEach
            FunputSection(title = stringResource(group.titleRes)) {
                inGroup.forEachIndexed { index, role ->
                    if (index > 0) FunputDivider()
                    ColorRow(
                        label = stringResource(role.labelRes),
                        color = role.read(theme),
                        onClick = { editing = role },
                        summary = role.follows?.takeIf { ThemeColorLinks.isAutomatic(role, theme) }?.let { source ->
                            stringResource(R.string.custom_theme_color_automatic, stringResource(source.labelRes))
                        },
                    )
                }
            }
        }
        if (hasDetail) {
            FunputButton(
                text = stringResource(
                    if (showsDetail) R.string.custom_theme_color_hide_advanced else R.string.custom_theme_color_show_advanced,
                ),
                onClick = { showsDetail = !showsDetail },
                style = FunputButtonStyle.PLAIN,
            )
        }
    }
    editing?.let { role ->
        ColorPickerSheet(
            title = stringResource(role.labelRes),
            initialColor = role.read(theme),
            onDismiss = { editing = null },
            onConfirm = { color ->
                onColorChange(role, color)
                editing = null
            },
        )
    }
}

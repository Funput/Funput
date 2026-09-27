package app.funput.funput.ui.theme.custom.studio

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputTextField
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.settings.PickerOption
import app.funput.funput.ui.settings.PickerSheet
import app.funput.funput.ui.theme.custom.ThemeDraftState
import app.funput.funput.ui.theme.custom.color.ColorRow
import app.funput.funput.ui.theme.custom.color.picker.ColorPickerSheet
import app.funput.funput.ui.theme.custom.metrics.ThemeShapeSection

/**
 * Page "Chung": the theme as a whole. Its name, what it starts from, its accent, and the shape of
 * its keys; the first decisions, so they open the editor.
 */
@Composable
internal fun ThemeGeneralPage(state: ThemeDraftState, baseThemes: List<KeyboardThemeDescriptor>) {
    var pickingBase by remember { mutableStateOf(false) }
    var pickingAccent by remember { mutableStateOf(false) }
    FunputTextField(
        value = state.name,
        onValueChange = { state.name = it },
        label = stringResource(R.string.custom_theme_name_label),
        placeholder = stringResource(R.string.custom_theme_name_placeholder),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.testTag(ThemeNameTag),
    )
    FunputSection(title = null) {
        LinkRow(
            title = stringResource(R.string.custom_theme_base_title),
            value = state.baseTheme.name,
            onClick = { pickingBase = true },
        )
        FunputDivider()
        ColorRow(
            label = stringResource(R.string.custom_theme_accent_title),
            color = state.theme.accentColor,
            onClick = { pickingAccent = true },
            modifier = Modifier.testTag(AccentColorTag),
        )
    }
    ThemeShapeSection(state.theme, state::updateTheme)
    if (pickingBase) {
        PickerSheet(
            title = stringResource(R.string.custom_theme_base_title),
            options = baseThemes.map { PickerOption(it.id.value, it.name) },
            selected = state.baseThemeValue,
            onSelected = { value ->
                state.selectBaseTheme(value)
                pickingBase = false
            },
            onDismiss = { pickingBase = false },
        )
    }
    if (pickingAccent) {
        ColorPickerSheet(
            title = stringResource(R.string.custom_theme_accent_title),
            initialColor = state.theme.accentColor,
            onDismiss = { pickingAccent = false },
            onConfirm = { color ->
                state.applyAccent(color)
                pickingAccent = false
            },
        )
    }
}

/** Test tag of the theme name field. */
internal const val ThemeNameTag = "custom-theme-name"

/** Test tag of the accent colour row. */
internal const val AccentColorTag = "custom-theme-accent"

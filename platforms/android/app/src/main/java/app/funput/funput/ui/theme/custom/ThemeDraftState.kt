package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.theme.store.custom.CustomThemeOverrides

/**
 * Every editable value in the theme editor, in one place.
 *
 * The editor grows a control at a time, and threading each one through the screen, the form and
 * the tab switch as its own parameter and callback does not scale. Controls read and write this
 * holder directly, so adding a knob touches the holder and the one card that shows it.
 */
@Stable
internal class ThemeDraftState(
    private val baseThemes: List<KeyboardThemeDescriptor>,
    editingTheme: KeyboardThemeDescriptor?,
) {
    var name by mutableStateOf(editingTheme.initialThemeName())
    var baseThemeValue by mutableStateOf(editingTheme.initialBaseThemeValue())
    var accentColor by mutableIntStateOf(editingTheme.initialAccentColor())
    var keyBackgroundOpacity by mutableFloatStateOf(editingTheme.initialKeyBackgroundOpacity())
    var backgroundImageSource by mutableStateOf(editingTheme.initialBackgroundImageSource())
    var imageOpacity by mutableFloatStateOf(editingTheme.initialBackgroundImageOpacity())

    val baseTheme: KeyboardThemeDescriptor
        get() = baseThemes.find { theme -> theme.id.value == baseThemeValue } ?: baseThemes.first()

    /** Resolved tokens for both the live preview and the saved theme, so they cannot diverge. */
    val theme: KeyboardTheme
        get() = CustomThemeOverrides(
            accentColor = accentColor,
            keyBackgroundOpacity = keyBackgroundOpacity,
        ).applyTo(baseTheme.theme)

    val canSave: Boolean get() = name.trim().isNotEmpty()

    fun toDraft(): CustomThemeDraft = CustomThemeDraft(
        theme = theme,
        name = name,
        baseThemeId = baseTheme.id,
        backgroundImage = backgroundImageSource?.let { source ->
            KeyboardThemeBackgroundImage(source = source, opacity = imageOpacity)
        },
    )

    internal companion object {
        fun saver(
            baseThemes: List<KeyboardThemeDescriptor>,
            editingTheme: KeyboardThemeDescriptor?,
        ) = listSaver<ThemeDraftState, Any?>(
            save = { state ->
                listOf(
                    state.name,
                    state.baseThemeValue,
                    state.accentColor,
                    state.keyBackgroundOpacity,
                    state.backgroundImageSource,
                    state.imageOpacity,
                )
            },
            restore = { values ->
                ThemeDraftState(baseThemes, editingTheme).apply {
                    name = values[0] as String
                    baseThemeValue = values[1] as String
                    accentColor = values[2] as Int
                    keyBackgroundOpacity = values[3] as Float
                    backgroundImageSource = values[4] as String?
                    imageOpacity = values[5] as Float
                }
            },
        )
    }
}

@Composable
internal fun rememberThemeDraftState(
    baseThemes: List<KeyboardThemeDescriptor>,
    editingTheme: KeyboardThemeDescriptor?,
): ThemeDraftState = rememberSaveable(
    editingTheme?.id?.value,
    saver = ThemeDraftState.saver(baseThemes, editingTheme),
) {
    ThemeDraftState(baseThemes, editingTheme)
}

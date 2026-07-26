package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.theme.store.json.KeyboardThemeJson

/**
 * Every editable value in the theme editor, in one place.
 *
 * [theme] holds the tokens directly rather than deriving them from a base plus a couple of knobs,
 * so a control can address any token without the state holder growing a field per control. The
 * base theme is now only a starting point and the target of "restore the original theme".
 */
@Stable
internal class ThemeDraftState(
    private val baseThemes: List<KeyboardThemeDescriptor>,
    editingTheme: KeyboardThemeDescriptor?,
) {
    var name by mutableStateOf(editingTheme.initialThemeName())
    var baseThemeValue by mutableStateOf(editingTheme.initialBaseThemeValue())
        private set
    var theme by mutableStateOf(editingTheme?.theme ?: initialBaseTheme().theme)
    var backgroundImage by mutableStateOf(editingTheme?.backgroundImage)

    val baseTheme: KeyboardThemeDescriptor
        get() = baseThemes.find { it.id.value == baseThemeValue } ?: baseThemes.first()

    val canSave: Boolean get() = name.trim().isNotEmpty()

    /** Switching base replaces every token, which is also how "restore the original" works. */
    fun selectBaseTheme(value: String) {
        baseThemeValue = value
        theme = baseTheme.theme
    }

    fun updateTheme(transform: (KeyboardTheme) -> KeyboardTheme) {
        theme = transform(theme)
    }

    /** Applies a framing change; a no-op when no image has been chosen yet. */
    fun updateBackgroundImage(
        transform: (KeyboardThemeBackgroundImage) -> KeyboardThemeBackgroundImage,
    ) {
        backgroundImage = backgroundImage?.let(transform)
    }

    fun selectBackgroundImage(source: String) {
        backgroundImage = KeyboardThemeBackgroundImage(
            source = source,
            opacity = backgroundImage?.opacity ?: DefaultBackgroundImageOpacity,
        )
    }

    fun toDraft(): CustomThemeDraft = CustomThemeDraft(
        theme = theme,
        name = name,
        baseThemeId = baseTheme.id,
        backgroundImage = backgroundImage,
    )

    private fun initialBaseTheme(): KeyboardThemeDescriptor =
        baseThemes.find { it.id.value == baseThemeValue } ?: baseThemes.first()

    internal companion object {
        fun saver(
            baseThemes: List<KeyboardThemeDescriptor>,
            editingTheme: KeyboardThemeDescriptor?,
        ) = listSaver<ThemeDraftState, Any?>(
            save = { state ->
                val image = state.backgroundImage
                listOf(
                    state.name,
                    state.baseThemeValue,
                    KeyboardThemeJson.encode(state.theme),
                    image?.source,
                    image?.opacity ?: 0f,
                    image?.focalX ?: 0f,
                    image?.focalY ?: 0f,
                    image?.zoom ?: KeyboardThemeBackgroundImage.MinZoom,
                    image?.blurRadiusDp ?: 0f,
                    image?.overlayColor ?: KeyboardThemeBackgroundImage.Transparent,
                )
            },
            restore = { values ->
                ThemeDraftState(baseThemes, editingTheme).apply {
                    name = values[0] as String
                    baseThemeValue = values[1] as String
                    theme = KeyboardThemeJson.decode(values[2] as String)
                    backgroundImage = (values[3] as String?)?.let { source ->
                        KeyboardThemeBackgroundImage(
                            source = source,
                            opacity = values[4] as Float,
                            focalX = values[5] as Float,
                            focalY = values[6] as Float,
                            zoom = values[7] as Float,
                            blurRadiusDp = values[8] as Float,
                            overlayColor = values[9] as Int,
                        )
                    }
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

package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardTheme

/** Optional visual token overrides for a user-created theme. */
data class CustomThemeOverrides(
    val accentColor: Int? = null,
) {
    fun applyTo(baseTheme: KeyboardTheme): KeyboardTheme =
        baseTheme.copy(
            accentColor = accentColor ?: baseTheme.accentColor,
        )
}

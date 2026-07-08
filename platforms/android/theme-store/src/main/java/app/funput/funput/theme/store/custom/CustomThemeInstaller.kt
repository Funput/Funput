package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.CustomKeyboardThemeStore

/** Builds and saves custom themes for UI flows. */
class CustomThemeInstaller(
    private val store: CustomKeyboardThemeStore,
    private val builder: CustomThemeBuilder = CustomThemeBuilder(),
) {
    fun install(
        draft: CustomThemeDraft,
        baseTheme: KeyboardThemeDescriptor,
        existingThemeIds: Set<KeyboardThemeId>,
    ): KeyboardThemeDescriptor {
        val descriptor = builder.build(draft, baseTheme, existingThemeIds)
        store.upsertTheme(descriptor)
        return descriptor
    }

    fun update(
        themeId: KeyboardThemeId,
        draft: CustomThemeDraft,
        baseTheme: KeyboardThemeDescriptor,
    ): KeyboardThemeDescriptor {
        val descriptor = builder.build(
            draft = draft,
            baseTheme = baseTheme,
            existingThemeIds = emptySet(),
            themeId = themeId,
        )
        store.upsertTheme(descriptor)
        return descriptor
    }
}

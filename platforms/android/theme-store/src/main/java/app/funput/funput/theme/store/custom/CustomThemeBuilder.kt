package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin

/** Converts a validated custom theme draft into an installable theme descriptor. */
class CustomThemeBuilder(
    private val idGenerator: CustomThemeIdGenerator = CustomThemeIdGenerator(),
) {
    fun build(
        draft: CustomThemeDraft,
        baseTheme: KeyboardThemeDescriptor,
        existingThemeIds: Set<KeyboardThemeId>,
        themeId: KeyboardThemeId? = null,
    ): KeyboardThemeDescriptor {
        require(draft.baseThemeId == baseTheme.id) {
            "Draft base theme must match the resolved base theme"
        }

        val name = draft.normalizedName()
        val author = draft.normalizedAuthor()
        require(name.isNotBlank()) { "Custom theme name must not be blank" }
        require(author.isNotBlank()) { "Custom theme author must not be blank" }

        return KeyboardThemeDescriptor(
            id = themeId ?: idGenerator.generate(name, existingThemeIds),
            version = 1,
            name = name,
            author = author,
            origin = KeyboardThemeOrigin.CUSTOM,
            baseThemeId = draft.baseThemeId,
            backgroundImage = draft.backgroundImage,
            theme = draft.overrides.applyTo(baseTheme.theme),
        )
    }
}

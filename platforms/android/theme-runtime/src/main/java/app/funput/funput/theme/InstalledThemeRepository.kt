package app.funput.funput.theme

/**
 * Aggregates all themes currently installed on the device.
 *
 * New sources for downloaded packs or user-created themes can be added without changing callers.
 */
class InstalledThemeRepository(
    private val sources: List<InstalledThemeSource>,
    private val defaultThemeId: KeyboardThemeId = KeyboardThemeId.Default,
) {
    constructor(
        vararg sources: InstalledThemeSource,
        defaultThemeId: KeyboardThemeId = KeyboardThemeId.Default,
    ) : this(sources.toList(), defaultThemeId)

    val themes: List<KeyboardThemeDescriptor>
        get() = currentCatalog().themes

    val defaultTheme: KeyboardThemeDescriptor
        get() = currentCatalog().defaultTheme

    fun find(id: KeyboardThemeId): KeyboardThemeDescriptor? = currentCatalog().find(id)

    fun resolve(id: KeyboardThemeId): KeyboardThemeDescriptor = currentCatalog().resolve(id)

    private fun currentCatalog(): KeyboardThemeCatalog =
        KeyboardThemeCatalog(
            themes = sources.flatMap(InstalledThemeSource::loadThemes),
            defaultThemeId = defaultThemeId,
        )

    companion object {
        fun builtIn(): InstalledThemeRepository =
            InstalledThemeRepository(BuiltInKeyboardThemeSource)
    }
}

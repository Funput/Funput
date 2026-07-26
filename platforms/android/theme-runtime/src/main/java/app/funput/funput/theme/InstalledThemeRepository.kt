package app.funput.funput.theme

/**
 * Aggregates all themes currently installed on the device.
 *
 * New sources for downloaded packs or user-created themes can be added without changing callers.
 *
 * Every accessor re-reads its sources, which for the custom-theme source means touching disk. That
 * is deliberate: the settings app and the IME hold separate repositories, so a cached catalog in
 * one would not learn about a theme the other just saved, and the keyboard would quietly keep
 * drawing a theme the user had already replaced. Callers that need several answers about the same
 * moment should take a [snapshot] and query that instead of calling the repository repeatedly.
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
        get() = snapshot().themes

    val defaultTheme: KeyboardThemeDescriptor
        get() = snapshot().defaultTheme

    fun find(id: KeyboardThemeId): KeyboardThemeDescriptor? = snapshot().find(id)

    fun resolve(id: KeyboardThemeId): KeyboardThemeDescriptor = snapshot().resolve(id)

    /**
     * Reads every source once and returns the result.
     *
     * Holding the returned catalog across a single screen or one keyboard refresh collapses what
     * would otherwise be one disk read per question, without introducing a cache that could go
     * stale behind another process.
     */
    fun snapshot(): KeyboardThemeCatalog = KeyboardThemeCatalog(
        themes = sources.flatMap(InstalledThemeSource::loadThemes),
        defaultThemeId = defaultThemeId,
    )

    companion object {
        fun builtIn(): InstalledThemeRepository =
            InstalledThemeRepository(BuiltInKeyboardThemeSource)
    }
}

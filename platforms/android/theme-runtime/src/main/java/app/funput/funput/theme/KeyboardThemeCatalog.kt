package app.funput.funput.theme

/**
 * Immutable collection of themes available to the runtime.
 *
 * Keeping lookup and fallback behavior here means the IME never has to handle a missing theme.
 * A future installed-theme repository can construct the same catalog type from verified packages.
 */
class KeyboardThemeCatalog(
    themes: List<KeyboardThemeDescriptor>,
    defaultThemeId: KeyboardThemeId,
) {
    val themes: List<KeyboardThemeDescriptor> = themes.toList()

    private val themesById: Map<KeyboardThemeId, KeyboardThemeDescriptor>

    val defaultTheme: KeyboardThemeDescriptor

    init {
        require(this.themes.isNotEmpty()) { "A theme catalog must not be empty" }

        themesById = this.themes.associateBy(KeyboardThemeDescriptor::id)
        require(themesById.size == this.themes.size) { "Theme identifiers must be unique" }

        defaultTheme = requireNotNull(themesById[defaultThemeId]) {
            "Default theme must be present in the catalog"
        }
    }

    fun find(id: KeyboardThemeId): KeyboardThemeDescriptor? = themesById[id]

    fun resolve(id: KeyboardThemeId): KeyboardThemeDescriptor = find(id) ?: defaultTheme
}

/** The themes shipped with the application and always available offline. */
object LocalKeyboardThemeCatalog {
    private const val FunputAuthor = "Funput"

    private val catalog = KeyboardThemeCatalog(
        themes = listOf(
            KeyboardThemeDescriptor(
                id = KeyboardThemeId.Dark,
                version = 1,
                name = "Dark",
                author = FunputAuthor,
                theme = KeyboardThemes.Dark,
            ),
            KeyboardThemeDescriptor(
                id = KeyboardThemeId.Light,
                version = 1,
                name = "Light",
                author = FunputAuthor,
                theme = KeyboardThemes.Light,
            ),
        ),
        defaultThemeId = KeyboardThemeId.Default,
    )

    val themes: List<KeyboardThemeDescriptor>
        get() = catalog.themes

    val defaultTheme: KeyboardThemeDescriptor
        get() = catalog.defaultTheme

    fun find(id: KeyboardThemeId): KeyboardThemeDescriptor? = catalog.find(id)

    fun resolve(id: KeyboardThemeId): KeyboardThemeDescriptor = catalog.resolve(id)
}

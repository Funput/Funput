package app.funput.funput.theme

/**
 * Immutable collection of themes available to the runtime.
 *
 * Keeping lookup and fallback behavior here means the IME never has to handle a missing theme.
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
    private val repository = InstalledThemeRepository.builtIn()

    val themes: List<KeyboardThemeDescriptor>
        get() = repository.themes

    val defaultTheme: KeyboardThemeDescriptor
        get() = repository.defaultTheme

    fun find(id: KeyboardThemeId): KeyboardThemeDescriptor? = repository.find(id)

    fun resolve(id: KeyboardThemeId): KeyboardThemeDescriptor = repository.resolve(id)
}

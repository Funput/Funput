package app.funput.funput.theme

/** Themes shipped with the application and always available offline. */
object BuiltInKeyboardThemeSource : InstalledThemeSource {
    private const val FunputAuthor = "Funput"

    override fun loadThemes(): List<KeyboardThemeDescriptor> = listOf(
        KeyboardThemeDescriptor(
            id = KeyboardThemeId.Dark,
            version = 2,
            name = "Ink",
            author = FunputAuthor,
            origin = KeyboardThemeOrigin.BUILT_IN,
            theme = KeyboardThemes.Ink,
        ),
        KeyboardThemeDescriptor(
            id = KeyboardThemeId.Light,
            version = 2,
            name = "Paper",
            author = FunputAuthor,
            origin = KeyboardThemeOrigin.BUILT_IN,
            theme = KeyboardThemes.Paper,
        ),
        KeyboardThemeDescriptor(
            id = KeyboardThemeId.GlassDark,
            version = 1,
            name = "Glass Dark",
            author = FunputAuthor,
            origin = KeyboardThemeOrigin.BUILT_IN,
            theme = KeyboardThemes.GlassDark,
        ),
        KeyboardThemeDescriptor(
            id = KeyboardThemeId.GlassLight,
            version = 1,
            name = "Glass Light",
            author = FunputAuthor,
            origin = KeyboardThemeOrigin.BUILT_IN,
            theme = KeyboardThemes.GlassLight,
        ),
    )
}

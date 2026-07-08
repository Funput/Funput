package app.funput.funput.theme

/** Themes shipped with the application and always available offline. */
object BuiltInKeyboardThemeSource : InstalledThemeSource {
    private const val FunputAuthor = "Funput"

    override fun loadThemes(): List<KeyboardThemeDescriptor> = listOf(
        KeyboardThemeDescriptor(
            id = KeyboardThemeId.Dark,
            version = 1,
            name = "Dark",
            author = FunputAuthor,
            origin = KeyboardThemeOrigin.BUILT_IN,
            theme = KeyboardThemes.Dark,
        ),
        KeyboardThemeDescriptor(
            id = KeyboardThemeId.Light,
            version = 1,
            name = "Light",
            author = FunputAuthor,
            origin = KeyboardThemeOrigin.BUILT_IN,
            theme = KeyboardThemes.Light,
        ),
    )
}

package app.funput.funput.theme

/** Source of installed keyboard themes. */
fun interface InstalledThemeSource {
    fun loadThemes(): List<KeyboardThemeDescriptor>
}

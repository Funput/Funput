package app.funput.funput.theme.store

import android.content.Context
import java.io.File

/** Creates the default private-file store for custom keyboard themes. */
fun Context.customKeyboardThemeStore(): CustomKeyboardThemeStore =
    CustomThemeFileStore(File(filesDir, CustomThemesDirectory))

private const val CustomThemesDirectory = "keyboard-themes/custom"

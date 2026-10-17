package app.funput.funput.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource

/**
 * The title of the settings page a section is drawn on, or null outside a page.
 *
 * A page made of one section would otherwise say its name twice, once in the top bar and again
 * as the section header right under it.
 */
internal val LocalSettingsPageTitle = staticCompositionLocalOf<String?> { null }

/** The header for a section titled [id]: none when the page around it already carries that title. */
@Composable
internal fun sectionTitle(@StringRes id: Int): String? {
    val title = stringResource(id)
    return title.takeUnless { it == LocalSettingsPageTitle.current }
}

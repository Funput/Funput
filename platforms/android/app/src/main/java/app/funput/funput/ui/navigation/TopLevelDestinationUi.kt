package app.funput.funput.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputTabIcons
import app.funput.funput.ui.kit.layout.FunputTab

/**
 * Label and icons for a tab. Separate from [TopLevelDestination] so the model stays free of UI.
 *
 * Two icons: the outlined form while unselected and the filled form while selected, so the
 * selection reads from the glyph itself as well as from the capsule behind it.
 */
internal data class TopLevelDestinationUi(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
)

internal val TopLevelDestination.ui: TopLevelDestinationUi
    get() = when (this) {
        TopLevelDestination.KEYBOARD -> TopLevelDestinationUi(
            label = R.string.nav_keyboard,
            icon = FunputTabIcons.Keyboard,
            selectedIcon = FunputTabIcons.KeyboardSelected,
        )
        TopLevelDestination.SHORTCUTS -> TopLevelDestinationUi(
            label = R.string.nav_shortcuts,
            icon = FunputTabIcons.Shortcuts,
            selectedIcon = FunputTabIcons.ShortcutsSelected,
        )
        TopLevelDestination.APPEARANCE -> TopLevelDestinationUi(
            label = R.string.nav_appearance,
            icon = FunputTabIcons.Appearance,
            selectedIcon = FunputTabIcons.AppearanceSelected,
        )
        TopLevelDestination.NEWS -> TopLevelDestinationUi(
            label = R.string.nav_news,
            icon = FunputTabIcons.News,
            selectedIcon = FunputTabIcons.NewsSelected,
        )
        TopLevelDestination.ME -> TopLevelDestinationUi(
            label = R.string.nav_me,
            icon = FunputTabIcons.Me,
            selectedIcon = FunputTabIcons.MeSelected,
        )
    }

/** The tabs as FunputUI tab bar items, in [TopLevelDestination] order. */
@Composable
internal fun appTabs(): List<FunputTab> = TopLevelDestination.entries.map { tab ->
    val ui = tab.ui
    FunputTab(label = stringResource(ui.label), icon = ui.icon, selectedIcon = ui.selectedIcon)
}

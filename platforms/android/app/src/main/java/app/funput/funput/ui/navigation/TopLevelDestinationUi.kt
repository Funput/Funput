package app.funput.funput.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.icons.FunputIcons
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
        TopLevelDestination.SETTINGS -> TopLevelDestinationUi(
            label = R.string.nav_settings,
            icon = FunputIcons.Settings,
            selectedIcon = FunputIcons.SettingsSelected,
        )
        TopLevelDestination.APPEARANCE -> TopLevelDestinationUi(
            label = R.string.nav_appearance,
            icon = FunputIcons.Appearance,
            selectedIcon = FunputIcons.AppearanceSelected,
        )
        TopLevelDestination.ABOUT -> TopLevelDestinationUi(
            label = R.string.nav_about,
            icon = FunputIcons.About,
            selectedIcon = FunputIcons.AboutSelected,
        )
    }

/** The three tabs as FunputUI tab bar items, in [TopLevelDestination] order. */
@Composable
internal fun appTabs(): List<FunputTab> = TopLevelDestination.entries.map { tab ->
    val ui = tab.ui
    FunputTab(label = stringResource(ui.label), icon = ui.icon, selectedIcon = ui.selectedIcon)
}

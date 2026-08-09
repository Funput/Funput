package app.funput.funput.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R

/**
 * Label and icons for a tab. Separate from [TopLevelDestination] so the model stays free of UI.
 *
 * Two icons, because Material draws an unselected tab outlined and the selected one filled. The
 * three tabs used to be a mixture — two filled, one outlined — which read as three icons drawn by
 * different hands, and left the selection resting entirely on the pill behind it.
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
            icon = R.drawable.ic_nav_settings,
            selectedIcon = R.drawable.ic_nav_settings_filled,
        )
        TopLevelDestination.APPEARANCE -> TopLevelDestinationUi(
            label = R.string.nav_appearance,
            icon = R.drawable.ic_nav_appearance,
            selectedIcon = R.drawable.ic_nav_appearance_filled,
        )
        TopLevelDestination.ABOUT -> TopLevelDestinationUi(
            label = R.string.nav_about,
            icon = R.drawable.ic_nav_about,
            selectedIcon = R.drawable.ic_nav_about_filled,
        )
    }

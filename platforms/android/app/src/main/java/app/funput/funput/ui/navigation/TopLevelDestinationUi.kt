package app.funput.funput.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.funput.funput.R

/** Label and icon for a tab. Separate from [TopLevelDestination] so the model stays free of UI. */
internal data class TopLevelDestinationUi(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
)

internal val TopLevelDestination.ui: TopLevelDestinationUi
    get() = when (this) {
        TopLevelDestination.SETTINGS -> TopLevelDestinationUi(R.string.nav_settings, R.drawable.ic_settings)
        TopLevelDestination.APPEARANCE -> TopLevelDestinationUi(R.string.nav_appearance, R.drawable.ic_appearance)
        TopLevelDestination.ABOUT -> TopLevelDestinationUi(R.string.nav_about, R.drawable.ic_info)
    }

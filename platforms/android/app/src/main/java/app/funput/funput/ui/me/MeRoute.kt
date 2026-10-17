package app.funput.funput.ui.me

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ui.AppVersionProvider
import app.funput.funput.ui.keyboard.openWebsite

/** Binds the "Tôi" tab to the app's own metadata and to whatever opens links on this device. */
@Composable
internal fun MeRoute(
    onOpenData: () -> Unit,
    onOpenLicenses: () -> Unit,
    tabBar: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    MeScreen(
        versionName = AppVersionProvider.versionName(context),
        onOpenLink = context::openWebsite,
        onOpenData = onOpenData,
        onOpenLicenses = onOpenLicenses,
        tabBar = tabBar,
    )
}

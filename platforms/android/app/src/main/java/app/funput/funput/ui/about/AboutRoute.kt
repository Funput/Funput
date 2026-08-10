package app.funput.funput.ui.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ui.AppVersionProvider
import app.funput.funput.ui.keyboard.openWebsite

/** Binds the about screen to the app's own metadata and to whatever opens links on this device. */
@Composable
internal fun AboutRoute() {
    val context = LocalContext.current
    AboutScreen(
        versionName = AppVersionProvider.versionName(context),
        onOpenLink = context::openWebsite,
    )
}

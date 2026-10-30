package app.funput.funput.ui.settings.enterkey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import app.funput.funput.ime.settings.entersend.EnterToSendSettings
import app.funput.funput.ui.settings.enterkey.components.AppIconSize
import kotlinx.coroutines.launch

/** The "Phím Enter" page, bound to the store the IME reads and to the launcher's apps. */
@Composable
internal fun EnterKeyRoute(onBack: () -> Unit) {
    EnterKeyScreen(rememberEnterKeyScreenState(), onBack)
}

@Composable
private fun rememberEnterKeyScreenState(): EnterKeyScreenState {
    val context = LocalContext.current
    val store = remember(context) { EnterToSendSettings(context) }
    val scope = rememberCoroutineScope()
    val packages by store.packages.collectAsState(initial = null)
    val iconSizePx = with(LocalDensity.current) { AppIconSize.roundToPx() }
    val installed by produceState<List<InstalledApp>?>(null, context, iconSizePx) {
        value = InstalledApps.load(context, iconSizePx)
    }
    return remember(packages, installed, store, scope) {
        EnterKeyScreenState(
            packages = packages,
            installed = installed,
            onAdd = { packageName -> scope.launch { store.add(packageName) } },
            onRemove = { packageName -> scope.launch { store.remove(packageName) } },
        )
    }
}

/** The names of the apps whose Enter key sends, for the hub's summary line. */
@Composable
internal fun rememberEnterToSendAppNames(): List<String> {
    val context = LocalContext.current
    val store = remember(context) { EnterToSendSettings(context) }
    val packages by store.packages.collectAsState(initial = emptySet())
    val names by produceState(emptyList<String>(), context, packages) {
        value = InstalledApps.labels(context, packages)
    }
    return names
}

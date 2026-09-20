package app.funput.funput.ui.shortcuts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.shortcuts.persistence.FileShortcutsStore

@Composable
internal fun ShortcutsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { FileShortcutsStore.from(context) }
    val model = remember(store, scope) { ShortcutsScreenModel(store, scope) }
    LaunchedEffect(model) { model.reload() }
    ShortcutsScreen(model = model, onBack = onBack)
}

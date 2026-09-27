package app.funput.funput.ui.shortcuts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStoring
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.theme.FunputUiTheme
import java.util.UUID

/** An in-memory store that counts saves and can be told to fail them. */
internal class ShortcutTestStore(initial: ShortcutLibrary) : ShortcutsStoring {
    @Volatile
    var value = initial
    var saveCount = 0
        private set
    var saveFailure: Throwable? = null

    override fun load() = value

    @Synchronized
    override fun save(library: ShortcutLibrary) {
        saveFailure?.let { throw it }
        value = library
        saveCount += 1
    }
}

/** A shortcut whose id is derived from [trigger], so tests can find its row by tag. */
internal fun testShortcut(trigger: String, expansion: String) = TextShortcut(
    id = UUID.nameUUIDFromBytes(trigger.toByteArray()),
    trigger = trigger,
    expansion = expansion,
)

/**
 * Shows the shortcuts screen over a [ShortcutTestStore] holding [initial] and waits until the
 * library has loaded, so tests start from the list (or its empty state) rather than the spinner.
 */
internal fun ComposeContentTestRule.showShortcuts(
    initial: ShortcutLibrary,
    isDark: Boolean = false,
): ShortcutTestStore {
    val store = ShortcutTestStore(initial)
    var model: ShortcutsScreenModel? = null
    setContent {
        val scope = rememberCoroutineScope()
        val screenModel = remember { ShortcutsScreenModel(store, scope).also { model = it } }
        LaunchedEffect(screenModel) { screenModel.reload() }
        FunputUiTheme(isDark = isDark) {
            CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
                ShortcutsScreen(screenModel, onBack = {})
            }
        }
    }
    waitUntil { model?.hasLoaded == true && model?.isLoading == false }
    return store
}

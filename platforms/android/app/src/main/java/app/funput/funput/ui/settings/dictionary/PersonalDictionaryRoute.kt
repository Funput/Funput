package app.funput.funput.ui.settings.dictionary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.funput.funput.ime.suggestions.dictionary.FilePersonalDictionaryStore
import app.funput.funput.ui.FunputSettingsState
import app.funput.funput.ui.settings.dictionary.model.PersonalDictionaryScreenModel
import kotlinx.coroutines.launch

/** The suggestion settings the dictionary page shows and changes, beside the words themselves. */
@Immutable
internal data class DictionarySettings(
    val suggestionsEnabled: Boolean,
    /** A wipe the keyboard has not carried out yet; see `PersonalSuggestionPreferences`. */
    val resetPending: Boolean,
    val onEnableSuggestions: () -> Unit,
    val onReset: () -> Unit,
)

/**
 * "Từ điển cá nhân" bound to the keyboard's store. The list is read again whenever the page
 * comes back to the front, since the keyboard learns and applies edits while the app is away.
 */
@Composable
internal fun PersonalDictionaryRoute(settings: FunputSettingsState, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = remember(context, scope) {
        PersonalDictionaryScreenModel(FilePersonalDictionaryStore.from(context), scope)
    }
    LifecycleResumeEffect(model) {
        model.reload()
        onPauseOrDispose {}
    }
    val preferences = settings.personalSuggestions
    val store = settings.personalSuggestionStore
    PersonalDictionaryScreen(
        model = model,
        settings = DictionarySettings(
            suggestionsEnabled = preferences.enabled,
            resetPending = preferences.isResetPending,
            onEnableSuggestions = { scope.launch { store.setEnabled(true) } },
            onReset = { scope.launch { store.requestReset() } },
        ),
        onBack = onBack,
    )
}

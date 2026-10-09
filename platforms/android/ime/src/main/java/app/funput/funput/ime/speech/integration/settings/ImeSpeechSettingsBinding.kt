package app.funput.funput.ime.speech.integration.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Separate observation keeps permission/model/UI concerns out of IME settings. */
internal class ImeSpeechSettingsBinding(
    private val enabled: Flow<Boolean>,
    private val apply: (Boolean) -> Unit,
) {
    fun observe(scope: CoroutineScope): Job = scope.launch {
        enabled.distinctUntilChanged().collect(apply)
    }
}

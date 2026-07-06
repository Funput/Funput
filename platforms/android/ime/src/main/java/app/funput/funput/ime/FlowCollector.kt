package app.funput.funput.ime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal fun <T> Flow<T>.collectIn(
    scope: CoroutineScope,
    onValue: (T) -> Unit,
) {
    scope.launch { collect(onValue) }
}

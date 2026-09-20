package app.funput.funput.ime.shortcuts

import android.content.Context
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.shortcuts.persistence.FileShortcutsStore
import kotlinx.coroutines.CoroutineScope

internal fun createImeShortcutsController(
    context: Context,
    scope: CoroutineScope,
    handler: ImeKeyActionHandler,
) = ImeShortcutsController(
    scope = scope,
    store = FileShortcutsStore.from(context),
    begin = handler::beginShortcutActivation,
    receive = handler::receiveShortcuts,
)

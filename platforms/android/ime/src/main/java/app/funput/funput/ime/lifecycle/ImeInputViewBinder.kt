package app.funput.funput.ime.lifecycle

import android.inputmethodservice.InputMethodService
import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.ImeKeyboardCallbackBinder
import app.funput.funput.ime.ImePlacementBinder
import app.funput.funput.ime.ImeSettingsController
import app.funput.funput.ime.SystemInputMethodSwitcher
import app.funput.funput.ime.applyImeState
import app.funput.funput.ime.installedImeThemeRepository
import app.funput.funput.ime.isDarkAppearance
import app.funput.funput.keyboard.ui.FunputKeyboardView
import app.funput.funput.keyboard.ui.emoji.EmojiCatalogPreloader
import kotlinx.coroutines.CoroutineScope

/** Owns view construction and binding; the service supplies lifecycle callbacks. */
internal class ImeInputViewBinder(
    private val service: InputMethodService,
    private val scope: CoroutineScope,
    private val session: () -> ImeEditingSession,
    private val settings: () -> ImeSettingsController,
) {
    private val switcher = SystemInputMethodSwitcher(service)
    private val themeRepository by lazy { service.installedImeThemeRepository() }
    var view: FunputKeyboardView? = null
        private set

    fun create(): FunputKeyboardView = FunputKeyboardView(service).also { keyboard ->
        view = keyboard
        update()
        val editing = session()
        ImeKeyboardCallbackBinder.bind(keyboard, editing.actionHandler, editing.editorRuntime,
            editing.suggestionService, switcher, editing.letterReturn)
        ImePlacementBinder.bind(keyboard, service, scope)
        editing.bindPanels(keyboard)
        EmojiCatalogPreloader.schedule(keyboard)
    }

    fun update() {
        val keyboard = view ?: return
        val editing = session()
        keyboard.applyImeState(settings(), editing.editorRuntime.policy,
            editing.actionHandler.language, themeRepository, service.isDarkAppearance())
        editing.actionHandler.smartGesturesEnabled = keyboard.areSmartGesturesEnabled
    }

    fun detach() {
        view = null
    }
}

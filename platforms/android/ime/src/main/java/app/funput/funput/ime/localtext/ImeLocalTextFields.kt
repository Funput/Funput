package app.funput.funput.ime.localtext

import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.FunputKeyboardView

/**
 * The engine-backed fields behind the keyboard's panel searches, each with an engine of
 * its own. They start from the document's options ([documentEngine]) and language switch,
 * but not from the host field's policy: a URL field still searches emoji in Vietnamese.
 */
internal class ImeLocalTextFields(
    documentEngine: NativeVietnameseEngine,
    language: () -> KeyboardLanguage,
) : AutoCloseable {
    val emojiSearch = LocalTextComposer(NativeVietnameseEngine()) {
        LocalTextSettings(documentEngine.configuration, language() == KeyboardLanguage.VIETNAMESE)
    }

    fun attach(view: FunputKeyboardView) {
        view.localTextFields.emojiSearch = emojiSearch
    }

    override fun close() = emojiSearch.close()
}

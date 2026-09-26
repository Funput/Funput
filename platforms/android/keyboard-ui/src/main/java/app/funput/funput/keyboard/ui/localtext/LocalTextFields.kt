package app.funput.funput.keyboard.ui.localtext

/**
 * The fields the keyboard's panels type into, one per panel search. The IME replaces
 * them with engine-backed fields; panels read them lazily, so that can happen at any time.
 */
class LocalTextFields {
    var emojiSearch: LocalTextComposing = PlainLocalTextComposer()
}

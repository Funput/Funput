package app.funput.funput.ime.editing.capitalization

import app.funput.funput.ime.nativebridge.FunputNative

/**
 * Whether the caret is somewhere an upper-case letter belongs.
 *
 * An interface so unit tests can answer without loading the native library; the
 * answer itself is never written twice, because [NativeCapitalizationBoundary] is
 * the only implementation that ships and it forwards to the Rust rules every
 * Funput platform shares.
 */
internal fun interface CapitalizationBoundary {
    /** [before] is the text to the left of the caret; empty means start of document. */
    fun uppercases(mode: AutoCapitalizationMode, before: CharSequence): Boolean
}

internal object NativeCapitalizationBoundary : CapitalizationBoundary {
    override fun uppercases(mode: AutoCapitalizationMode, before: CharSequence): Boolean =
        when (mode) {
            AutoCapitalizationMode.NONE -> false
            AutoCapitalizationMode.ALL_CHARACTERS -> true
            AutoCapitalizationMode.WORDS -> FunputNative.nativeStartsWord(before.toString())
            AutoCapitalizationMode.SENTENCES -> FunputNative.nativeStartsSentence(before.toString())
        }
}

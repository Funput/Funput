package app.funput.funput.ime.shortcuts.undo

import app.funput.funput.ime.nativebridge.FunputNative
import app.funput.funput.ime.nativebridge.correction.CorrectionEdit

/** The engine's one-tap undo of the gõ tắt expansion just written. */
internal interface ShortcutUndoEngine {
    fun isArmed(): Boolean = false

    /** The word the chip names, without the boundary character. */
    fun text(): String? = null

    /** Expansion plus the boundary character still sitting before the caret. */
    fun suffix(): String? = null

    fun undo(): CorrectionEdit? = null

    fun dismiss() = Unit
}

internal object IdleShortcutUndo : ShortcutUndoEngine

/** [ShortcutUndoEngine] over the JNI calls, on the handle [handle] hands out. */
internal class NativeShortcutUndo(private val handle: () -> Long) : ShortcutUndoEngine {
    override fun isArmed() = FunputNative.nativeHasShortcutUndo(handle())

    override fun text() = FunputNative.nativeShortcutUndoText(handle()).ifEmpty { null }

    override fun suffix() = FunputNative.nativeShortcutUndoSuffix(handle()).ifEmpty { null }

    override fun undo() = CorrectionEdit.decode(FunputNative.nativeUndoShortcut(handle()))

    override fun dismiss() = FunputNative.nativeDismissShortcutUndo(handle())
}

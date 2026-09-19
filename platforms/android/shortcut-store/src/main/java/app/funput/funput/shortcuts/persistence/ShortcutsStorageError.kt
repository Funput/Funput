package app.funput.funput.shortcuts.persistence

/** Stable failure categories callers can present without exposing file paths or contents. */
sealed class ShortcutsStorageError : Exception() {
    data object Unavailable : ShortcutsStorageError()
    data object ReadFailed : ShortcutsStorageError()
    data object WriteFailed : ShortcutsStorageError()
    data object InvalidData : ShortcutsStorageError()
    data object UnsupportedVersion : ShortcutsStorageError()
    data object DuplicateTrigger : ShortcutsStorageError()
}

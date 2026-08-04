package app.funput.funput.ime.editing

internal enum class CompositionRenderMode {
    COMPOSING,
    COMMITTED,
    /** Like [COMMITTED], but shrink/replace via KEYCODE_DEL (ONLYOFFICE-style hosts). */
    COMMITTED_KEY_DELETE,
}

internal val CompositionRenderMode.deleteWithKeyEvents: Boolean
    get() = this == CompositionRenderMode.COMMITTED_KEY_DELETE

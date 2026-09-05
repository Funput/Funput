package app.funput.funput.ime.editing

internal enum class CompositionRenderMode {
    COMPOSING,
    COMMITTED,
    /** Like [COMMITTED], but shrink/replace via KEYCODE_DEL (ONLYOFFICE-style hosts). */
    COMMITTED_KEY_DELETE,
    /**
     * Linux-sandbox / PC-framework hosts (WPS Office PC). They ignore InputConnection
     * text APIs; the engine buffer is written only as KeyEvents.
     */
    KEY_EVENT,
}

internal val CompositionRenderMode.deleteWithKeyEvents: Boolean
    get() = this == CompositionRenderMode.COMMITTED_KEY_DELETE

internal val CompositionRenderMode.usesComposingSpans: Boolean
    get() = this == CompositionRenderMode.COMPOSING

internal val CompositionRenderMode.writesKeyEvents: Boolean
    get() = this == CompositionRenderMode.KEY_EVENT

package app.funput.funput.ime.editing

/**
 * Selects editors that need committed buffer replacement instead of composing spans.
 *
 * Prefixes cover release variants such as Firefox Beta, Facebook Lite, Messenger, Instagram,
 * Threads, Reddit, and ONLYOFFICE Documents builds without coupling the editing pipeline to
 * individual product names.
 */
internal object CompositionCompatibilityPolicy {
    private val committedPackagePrefixes = listOf(
        "org.mozilla.",
        "com.facebook.",
        "com.instagram.",
        "com.reddit.",
    )

    private val keyDeletePackagePrefixes = listOf(
        "com.onlyoffice.",
    )

    fun renderMode(packageName: String?): CompositionRenderMode = when {
        keyDeletePackagePrefixes.any { packageName?.startsWith(it) == true } ->
            CompositionRenderMode.COMMITTED_KEY_DELETE
        committedPackagePrefixes.any { packageName?.startsWith(it) == true } ->
            CompositionRenderMode.COMMITTED
        else -> CompositionRenderMode.COMPOSING
    }
}

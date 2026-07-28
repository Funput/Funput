package app.funput.funput.ime.editing

/**
 * Selects editors that need committed buffer replacement instead of composing spans.
 *
 * Prefixes cover release variants such as Firefox Beta, Facebook Lite, Messenger, Instagram,
 * Threads, and Reddit builds without coupling the editing pipeline to individual product names.
 */
internal object CompositionCompatibilityPolicy {
    private val committedPackagePrefixes = listOf(
        "org.mozilla.",
        "com.facebook.",
        "com.instagram.",
        "com.reddit.",
    )

    fun renderMode(packageName: String?): CompositionRenderMode =
        if (committedPackagePrefixes.any { packageName?.startsWith(it) == true }) {
            CompositionRenderMode.COMMITTED
        } else {
            CompositionRenderMode.COMPOSING
        }
}

package app.funput.funput.ime.editing

import app.funput.funput.ime.editing.keyevent.KeyEventHosts

/**
 * Selects editors that cannot host composing spans, without changing the default pipeline.
 *
 * Each render mode owns its host table. KEY_EVENT packages live in [KeyEventHosts] so
 * OEM sandbox engines can be added without growing this dispatcher.
 *
 * Prefixes cover release variants such as Firefox Beta, Facebook Lite, Messenger, Instagram,
 * Threads, Reddit, and ONLYOFFICE Documents without coupling the pipeline to product names.
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

    fun renderMode(packageName: String?): CompositionRenderMode =
        KeyEventHosts.modeFor(packageName)
            ?: keyDeleteMode(packageName)
            ?: committedMode(packageName)
            ?: CompositionRenderMode.COMPOSING

    private fun keyDeleteMode(packageName: String?) =
        CompositionRenderMode.COMMITTED_KEY_DELETE.takeIf {
            keyDeletePackagePrefixes.any { packageName?.startsWith(it) == true }
        }

    private fun committedMode(packageName: String?) =
        CompositionRenderMode.COMMITTED.takeIf {
            committedPackagePrefixes.any { packageName?.startsWith(it) == true }
        }
}

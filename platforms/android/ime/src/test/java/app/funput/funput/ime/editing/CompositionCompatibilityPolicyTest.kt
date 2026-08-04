package app.funput.funput.ime.editing

import org.junit.Assert.assertEquals
import org.junit.Test

class CompositionCompatibilityPolicyTest {
    @Test
    fun `Mozilla and social app variants use committed composition`() {
        val packages = listOf(
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "com.facebook.katana",
            "com.facebook.orca",
            "com.facebook.lite",
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.reddit.frontpage",
        )

        packages.forEach { packageName ->
            assertEquals(
                "$packageName should use committed composition",
                CompositionRenderMode.COMMITTED,
                CompositionCompatibilityPolicy.renderMode(packageName),
            )
        }
    }

    @Test
    fun `OnlyOffice uses key-event committed composition`() {
        assertEquals(
            CompositionRenderMode.COMMITTED_KEY_DELETE,
            CompositionCompatibilityPolicy.renderMode("com.onlyoffice.documents"),
        )
    }

    @Test
    fun `unlisted and missing packages keep native composition`() {
        listOf(null, "com.android.chrome", "com.whatsapp").forEach { packageName ->
            assertEquals(
                CompositionRenderMode.COMPOSING,
                CompositionCompatibilityPolicy.renderMode(packageName),
            )
        }
    }
}

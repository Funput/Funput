package app.funput.funput.ui.settings.setup

import app.funput.funput.ime.FunputImeComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardSetupInspectorTest {
    private val imeId = FunputImeComponent.id
    private val imeIdShort = FunputImeComponent.idShort

    @Test
    fun funputImeComponentMatchesShortAndLongIds() {
        assertTrue(FunputImeComponent.matches(imeId))
        assertTrue(FunputImeComponent.matches(imeIdShort))
        assertTrue(FunputImeComponent.matches("$imeIdShort:1234567890"))
    }

    @Test
    fun missingSettingsDefaultToNotEnabled() {
        assertEquals(
            KeyboardSetupStatus.NOT_ENABLED,
            KeyboardSetupInspector.resolve(false, null, null),
        )
        assertEquals(
            KeyboardSetupStatus.NOT_ENABLED,
            KeyboardSetupInspector.resolve(false, "", null),
        )
    }

    @Test
    fun enabledFromSystemOverridesMissingSettingsString() {
        assertEquals(
            KeyboardSetupStatus.NOT_SELECTED,
            KeyboardSetupInspector.resolve(true, null, "com.other.ime/.Service"),
        )
    }

    @Test
    fun enabledWithoutDefaultIsNotSelected() {
        val enabled = "com.other.ime/.Service;$imeId"

        assertEquals(
            KeyboardSetupStatus.NOT_SELECTED,
            KeyboardSetupInspector.resolve(enabled, "com.other.ime/.Service"),
        )
    }

    @Test
    fun enabledWithMatchingDefaultIsReady() {
        val enabled = "com.other.ime/.Service;$imeId"

        assertEquals(
            KeyboardSetupStatus.READY,
            KeyboardSetupInspector.resolve(enabled, imeId),
        )
    }

    @Test
    fun shortFormEnabledIdIsRecognized() {
        assertEquals(
            KeyboardSetupStatus.READY,
            KeyboardSetupInspector.resolve(imeIdShort, imeIdShort),
        )
    }

    @Test
    fun absentFromEnabledListIsNotEnabled() {
        assertEquals(
            KeyboardSetupStatus.NOT_ENABLED,
            KeyboardSetupInspector.resolve("com.other.ime/.Service", imeId),
        )
    }

    @Test
    fun enabledEntryWithSubtypeSuffixIsRecognized() {
        val enabled = "$imeIdShort:1234567890"

        assertEquals(
            KeyboardSetupStatus.NOT_SELECTED,
            KeyboardSetupInspector.resolve(enabled, "com.other.ime/.Service"),
        )
    }

    @Test
    fun defaultEntryWithSubtypeSuffixIsRecognized() {
        val enabled = "$imeIdShort:1234567890"

        assertEquals(
            KeyboardSetupStatus.READY,
            KeyboardSetupInspector.resolve(enabled, "$imeIdShort:9876543210"),
        )
    }
}

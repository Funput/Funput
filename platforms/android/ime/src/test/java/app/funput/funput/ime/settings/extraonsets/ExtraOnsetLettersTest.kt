package app.funput.funput.ime.settings.extraonsets

import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.SmartCompositionSettingCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Persistence normalization and the master/letter selection contract. */
class ExtraOnsetLettersTest {
    @Test fun `missing or empty selections stay off`() {
        listOf(null, "", "abc", "!? 123").forEach { value ->
            assertEquals(ExtraOnsetLetters.None, ExtraOnsetLetters.parse(value))
        }
        assertFalse(SmartCompositionPreferences.Default.extraOnsets.isEnabled)
        assertEquals(SmartCompositionPreferences.Default, SmartCompositionSettingCodec.decode(null, null, null))
    }

    @Test fun `decoding ignores case unknown letters and duplicates`() {
        val selection = ExtraOnsetLetters.parse("J!?WFzFZ")
        assertEquals("zfwj", selection.configurationValue)
        assertEquals(ExtraOnsetLetters.All, selection)
        assertEquals("fj", ExtraOnsetLetters.parse("j--FFF").configurationValue)
        assertEquals(listOf('z', 'f', 'w', 'j'), ExtraOnsetLetter.Ordered.map { it.spelling })
    }

    @Test fun `every possible subset round trips canonically`() {
        (0..15).forEach { subset ->
            var selection = ExtraOnsetLetters.None
            ExtraOnsetLetter.Ordered.forEachIndexed { index, letter ->
                selection = selection.withLetter(letter, subset and (1 shl index) != 0)
            }
            assertEquals(selection, ExtraOnsetLetters.parse(selection.configurationValue))
            assertEquals(
                selection,
                SmartCompositionSettingCodec.decode(null, null, null, selection.configurationValue).extraOnsets,
            )
        }
    }

    @Test fun `letter edits preserve peers and removing the last turns the master off`() {
        var selection = ExtraOnsetLetters.None.withLetter(ExtraOnsetLetter.J, true)
        assertTrue(selection.isEnabled)
        selection = selection.withLetter(ExtraOnsetLetter.Z, true)
        assertEquals("zj", selection.configurationValue)
        assertEquals(selection, selection.withLetter(ExtraOnsetLetter.J, true))
        selection = selection.withLetter(ExtraOnsetLetter.J, false)
        assertEquals("z", selection.configurationValue)
        assertFalse(selection.withLetter(ExtraOnsetLetter.Z, false).isEnabled)
    }
}

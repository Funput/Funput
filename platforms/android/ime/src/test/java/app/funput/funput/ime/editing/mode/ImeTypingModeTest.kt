package app.funput.funput.ime.editing.mode

import app.funput.funput.keyboard.model.KeyboardLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeTypingModeTest {
    @Test fun defaultModeComposesVietnamese() {
        val mode = ImeTypingMode()
        assertTrue(mode.usesVietnameseComposition)
        assertFalse(mode.usesEnglishShortcuts(true))
    }

    @Test fun englishRequiresShortcutActivationAndEditorPermission() {
        val mode = ImeTypingMode()
        mode.selectLanguage(KeyboardLanguage.ENGLISH)
        assertFalse(mode.usesVietnameseComposition)
        assertFalse(mode.usesEnglishShortcuts(false))
        assertTrue(mode.usesEnglishShortcuts(true))
        mode.configure(true, true, false)
        assertFalse(mode.usesEnglishShortcuts(true))
    }

    @Test fun secureEditorCannotChangeTheSelectedLanguage() {
        val mode = ImeTypingMode()
        mode.configure(false, false, false)
        mode.selectLanguage(KeyboardLanguage.ENGLISH)
        assertEquals(KeyboardLanguage.VIETNAMESE, mode.language)
        assertFalse(mode.usesVietnameseComposition)
        assertFalse(mode.suggestionsAllowed)
        assertFalse(mode.usesEnglishShortcuts(true))
    }

    @Test fun newEditorPolicyPreservesLanguageAndRestoresComposition() {
        val mode = ImeTypingMode()
        mode.selectLanguage(KeyboardLanguage.ENGLISH)
        mode.configure(false, false, false)
        assertFalse(mode.usesEnglishShortcuts(true))
        mode.configure(true, true, true)
        assertEquals(KeyboardLanguage.ENGLISH, mode.language)
        assertTrue(mode.usesEnglishShortcuts(true))
        mode.selectLanguage(KeyboardLanguage.VIETNAMESE)
        assertTrue(mode.usesVietnameseComposition)
    }

    @Test fun disablingSuggestionsDoesNotDisableTypingOrShortcuts() {
        val mode = ImeTypingMode()
        mode.configure(true, false, true)
        assertTrue(mode.usesVietnameseComposition)
        assertFalse(mode.suggestionsAllowed)
        mode.selectLanguage(KeyboardLanguage.ENGLISH)
        assertTrue(mode.usesEnglishShortcuts(true))
    }
}

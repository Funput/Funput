package app.funput.funput.ui.settings.summary

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.extraonsets.ExtraOnsetLetters
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.testSettingsState
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The sentence under each hub row, word for word, and never with a dot or dash separator. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsSummariesTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val summaries = SettingsSummaries(resources)

    @Test
    fun `typing names the tone style, and the extra onsets when some are allowed`() {
        val all = SmartCompositionPreferences.Default.copy(extraOnsets = ExtraOnsetLetters.All)
        val some = SmartCompositionPreferences.Default.copy(extraOnsets = ExtraOnsetLetters.parse("zw"))

        assertEquals("Đặt dấu kiểu cũ", summaries.of(SettingsPage.TYPING, testSettingsState()))
        assertEquals(
            "Đặt dấu kiểu cũ, cho phép z, f, w và j đầu từ",
            summaries.of(SettingsPage.TYPING, testSettingsState(smartComposition = all)),
        )
        assertEquals(
            "Đặt dấu kiểu cũ, cho phép z và w đầu từ",
            summaries.of(SettingsPage.TYPING, testSettingsState(smartComposition = some)),
        )
    }

    @Test
    fun `layout names the placement mode and the key height`() {
        assertEquals("Chế độ thường, phím cao 100%", summaries.of(SettingsPage.LAYOUT, testSettingsState()))
    }

    @Test
    fun `smart input counts what is on`() {
        // The defaults leave spell check off and the other five on.
        assertEquals("Đang bật 5 trên 6 tính năng", summaries.of(SettingsPage.SMART_INPUT, testSettingsState()))
    }

    @Test
    fun `feedback lists what is on, in a sentence`() {
        fun feedback(haptics: Boolean, sounds: Boolean, previews: Boolean = false) = summaries.of(
            SettingsPage.FEEDBACK,
            testSettingsState(hapticsEnabled = haptics, soundsEnabled = sounds, keyPreviewsEnabled = previews),
        )

        assertEquals("Đang tắt tất cả", feedback(haptics = false, sounds = false))
        assertEquals("Đang bật rung trung bình", feedback(haptics = true, sounds = false))
        assertEquals("Đang bật âm thanh", feedback(haptics = false, sounds = true))
        assertEquals("Đang bật rung trung bình và âm thanh", feedback(haptics = true, sounds = true))
        assertEquals("Đang bật xem trước phím", feedback(haptics = false, sounds = false, previews = true))
        assertEquals(
            "Đang bật rung trung bình, âm thanh và xem trước phím",
            feedback(haptics = true, sounds = true, previews = true),
        )
        val strong = testSettingsState(hapticStrength = KeyboardHapticStrength.STRONG, keyPreviewsEnabled = false)
        assertEquals("Đang bật rung mạnh", summaries.of(SettingsPage.FEEDBACK, strong))
    }

    @Test
    fun `clipboard says how long history is kept, or that it is not`() {
        val day = testSettingsState(clipboardExpiry = ClipboardExpiry.DAY)
        val off = testSettingsState(clipboardPreferences = ClipboardPreferences.Default.copy(enabled = false))

        assertEquals("Lưu lịch sử trong 1 ngày", summaries.of(SettingsPage.CLIPBOARD, day))
        assertEquals("Không lưu lịch sử", summaries.of(SettingsPage.CLIPBOARD, off))
    }

    @Test
    fun `data has no summary`() {
        assertNull(summaries.of(SettingsPage.DATA, testSettingsState()))
    }

    @Test
    fun `sentences join three items with a comma and the last with và`() {
        assertEquals("a, b và c", resources.joinAsSentence(listOf("a", "b", "c")))
        assertEquals("Đang bật tất cả", resources.countSentence(on = 6, total = 6))
        assertEquals("Đang tắt tất cả", resources.countSentence(on = 0, total = 6))
    }

    @Test
    fun `no summary uses a middle dot or a dash`() {
        val states = listOf(true, false).flatMap { haptics ->
            ClipboardExpiry.entries.map { expiry ->
                testSettingsState(hapticsEnabled = haptics, soundsEnabled = !haptics, clipboardExpiry = expiry)
            }
        }
        states.forEach { state ->
            SettingsPage.entries.mapNotNull { page -> summaries.of(page, state) }.forEach { summary ->
                listOf("·", "—", "–").forEach { mark -> assertFalse(summary, mark in summary) }
            }
        }
    }
}

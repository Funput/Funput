package app.funput.funput.ui.settings.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.uitesting.SCREENSHOT_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The settings search finds the same thing with or without accents, case, or English names. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = "vi")
class SettingsSearchMatcherTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val index = SettingsSearchIndex(resources, speechAvailable = true)

    private fun titles(query: String) = index.search(query).map { it.title }

    @Test
    fun `folding drops accents and case, and turns đ into d`() {
        assertEquals("dat dau", SettingsSearchMatcher.normalize("Đặt dấu"))
        assertEquals("tu dong", SettingsSearchMatcher.normalize("TỰ ĐỘNG"))
    }

    @Test
    fun `a query finds its setting with or without accents`() {
        assertTrue("Rung khi nhấn phím" in titles("rung"))
        assertTrue("Rung khi nhấn phím" in titles("RUNG KHI NHAN"))
        assertTrue("Kiểu đặt dấu" in titles("dat dau"))
        assertTrue("Kiểu đặt dấu" in titles("đặt dấu"))
    }

    @Test
    fun `english names find their settings`() {
        assertTrue("Rung khi nhấn phím" in titles("haptic"))
        assertTrue("Kiểu gõ" in titles("telex"))
        assertTrue("Từ đã học" in titles("reset"))
    }

    @Test
    fun `every word of the query has to match`() {
        assertTrue("Âm thanh phím" in titles("am thanh"))
        assertFalse("Rung khi nhấn phím" in titles("am thanh"))
        assertEquals(emptyList<String>(), titles("rung zzz"))
    }

    @Test
    fun `a blank query finds nothing`() {
        assertEquals(emptyList<String>(), titles(""))
        assertEquals(emptyList<String>(), titles("   "))
    }

    @Test
    fun `every page can be reached from the search, and each smart switch by its own title`() {
        assertEquals(SettingsPage.entries.toSet(), index.pages)
        assertTrue("Cử chỉ thông minh" in titles("cu chi thong minh"))
        assertTrue("Kiểm tra chính tả" in titles("chinh ta"))
    }

    @Test
    fun `voice input is not found where the build does not ship it`() {
        val withoutSpeech = SettingsSearchIndex(resources, speechAvailable = false)

        assertFalse(SettingsPage.SPEECH in withoutSpeech.pages)
        assertEquals(emptyList<SettingsSearchResult>(), withoutSpeech.search("giong noi"))
    }
}

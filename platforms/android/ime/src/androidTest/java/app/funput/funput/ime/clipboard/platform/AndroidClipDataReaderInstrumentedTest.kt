package app.funput.funput.ime.clipboard.platform

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import android.os.PersistableBundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidClipDataReaderInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun readsPlainHtmlUriAndFirstNonEmptyItem() {
        assertSuccess(ClipData.newPlainText("label", " Việt,\\\n "), " Việt,\\\n ", ClipboardContentKind.TEXT)
        assertSuccess(
            ClipData.newHtmlText("label", "Funput", "<b>Funput</b>"),
            "Funput",
            ClipboardContentKind.TEXT,
        )
        assertSuccess(
            ClipData.newRawUri("label", Uri.parse("https://funput.app/path")),
            "https://funput.app/path",
            ClipboardContentKind.LINK,
        )
        val multi = ClipData.newPlainText("label", "")
        multi.addItem(ClipData.Item("second"))
        assertSuccess(multi, "second", ClipboardContentKind.TEXT)
    }

    @Test
    fun enforcesUtf16BoundaryAndRejectsUnsupportedOrEmptyContent() {
        assertTrue(read(ClipData.newPlainText("label", "x".repeat(99_999))) is ClipboardReadResult.Success)
        assertTrue(read(ClipData.newPlainText("label", "x".repeat(100_000))) is ClipboardReadResult.Success)
        assertEquals(
            ClipboardReadResult.TooLarge,
            read(ClipData.newPlainText("label", "x".repeat(100_001))),
        )
        assertEquals(ClipboardReadResult.Empty, read(ClipData.newPlainText("label", "")))
        val image = ClipData(
            ClipDescription("image", arrayOf("image/png")),
            ClipData.Item(Uri.parse("content://example/image")),
        )
        assertEquals(ClipboardReadResult.Unsupported, read(image))
    }

    @Test
    fun readsSensitiveFlagWithoutEmbeddingPreviewInMetadata() {
        val clip = ClipData.newPlainText("label", "secret")
        clip.description.extras = PersistableBundle().apply {
            putBoolean(SensitiveClipboardKey, true)
        }
        val snapshot = clip.description.clipboardSnapshot()
        assertTrue(snapshot.isSensitive)
        assertEquals(ClipboardContentKind.TEXT, snapshot.kind)
        assertTrue((read(clip) as ClipboardReadResult.Success).isSensitive)
    }

    private fun read(clip: ClipData) = clip.readClipboardText(context, 100_000)

    private fun assertSuccess(
        clip: ClipData,
        text: String,
        kind: ClipboardContentKind,
    ) {
        val result = read(clip) as ClipboardReadResult.Success
        assertEquals(text, result.text)
        assertEquals(kind, result.kind)
    }
}

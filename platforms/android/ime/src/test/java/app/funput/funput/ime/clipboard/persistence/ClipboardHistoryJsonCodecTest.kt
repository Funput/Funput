package app.funput.funput.ime.clipboard.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClipboardHistoryJsonCodecTest {
    @Test
    fun `round trip preserves exact text and metadata`() {
        val entry = clipboardEntry(
            text = "  Xin, chào\\bạn\n(づ｡◕‿‿◕｡)づ  ",
            pinned = true,
            sourceToken = "generation-42",
        )
        val payload = ClipboardHistoryPayload("generation-42", listOf(entry))

        assertEquals(payload, ClipboardHistoryJsonCodec.decode(ClipboardHistoryJsonCodec.encode(payload)))
    }

    @Test
    fun `unknown schema is rejected`() {
        val json = """{"version":2,"lastCapturedSourceToken":null,"items":[]}"""

        assertThrows(IllegalArgumentException::class.java) {
            ClipboardHistoryJsonCodec.decode(json)
        }
    }

    @Test
    fun `malformed entry is rejected`() {
        val json = """
            {"version":1,"lastCapturedSourceToken":null,"items":[
              {"id":"bad","text":"x","capturedAtEpochMillis":1,
               "isPinned":false,"sourceToken":"source"}
            ]}
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            ClipboardHistoryJsonCodec.decode(json)
        }
    }
}

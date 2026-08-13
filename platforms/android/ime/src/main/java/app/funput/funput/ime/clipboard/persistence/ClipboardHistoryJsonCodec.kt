package app.funput.funput.ime.clipboard.persistence

import app.funput.funput.ime.clipboard.model.ClipboardEntry
import java.time.Instant
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

internal data class ClipboardHistoryPayload(
    val lastCapturedSourceToken: String?,
    val items: List<ClipboardEntry>,
) {
    companion object {
        val Empty = ClipboardHistoryPayload(null, emptyList())
    }
}

internal object ClipboardHistoryJsonCodec {
    private const val SchemaVersion = 1

    fun encode(payload: ClipboardHistoryPayload): String = JSONObject()
        .put("version", SchemaVersion)
        .put("lastCapturedSourceToken", payload.lastCapturedSourceToken ?: JSONObject.NULL)
        .put("items", JSONArray().apply { payload.items.forEach { put(encodeEntry(it)) } })
        .toString()

    fun decode(json: String): ClipboardHistoryPayload {
        val root = JSONObject(json)
        require(root.getInt("version") == SchemaVersion) { "Unsupported clipboard schema" }
        val token = if (root.isNull("lastCapturedSourceToken")) {
            null
        } else {
            root.getString("lastCapturedSourceToken").also {
                require(it.isNotBlank()) { "Clipboard source token must not be blank" }
            }
        }
        val values = root.getJSONArray("items")
        val items = List(values.length()) { index -> decodeEntry(values.getJSONObject(index)) }
        return ClipboardHistoryPayload(token, items)
    }

    private fun encodeEntry(entry: ClipboardEntry) = JSONObject()
        .put("id", entry.id.toString())
        .put("text", entry.text)
        .put("capturedAtEpochMillis", entry.capturedAt.toEpochMilli())
        .put("isPinned", entry.isPinned)
        .put("sourceToken", entry.sourceToken)

    private fun decodeEntry(json: JSONObject) = ClipboardEntry(
        id = UUID.fromString(json.getString("id")),
        text = json.getString("text"),
        capturedAt = Instant.ofEpochMilli(json.getLong("capturedAtEpochMillis")),
        isPinned = json.getBoolean("isPinned"),
        sourceToken = json.getString("sourceToken"),
    )
}

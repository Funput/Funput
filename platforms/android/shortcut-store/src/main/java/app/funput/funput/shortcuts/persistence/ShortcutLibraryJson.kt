package app.funput.funput.shortcuts.persistence

import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

internal object ShortcutLibraryJson {
    fun encode(library: ShortcutLibrary): String = JSONObject()
        .put("schemaVersion", library.schemaVersion)
        .put("entries", JSONArray().apply { library.entries.forEach { put(encodeEntry(it)) } })
        .put("isEnabled", library.isEnabled)
        .put("smartCase", library.smartCase)
        .put("inEnglish", library.inEnglish)
        .toString()

    fun decode(text: String): ShortcutLibrary {
        val root = JSONObject(text)
        val version = root.getInt("schemaVersion")
        if (version != ShortcutLibrary.CurrentSchemaVersion) {
            throw ShortcutsStorageError.UnsupportedVersion
        }
        val values = root.getJSONArray("entries")
        return ShortcutLibrary(
            entries = List(values.length()) { decodeEntry(values.getJSONObject(it)) },
            isEnabled = root.getBoolean("isEnabled"),
            smartCase = root.getBoolean("smartCase"),
            inEnglish = root.getBoolean("inEnglish"),
            schemaVersion = version,
        ).also(ShortcutLibrary::validate)
    }

    private fun encodeEntry(entry: TextShortcut) = JSONObject()
        .put("id", entry.id.toString())
        .put("trigger", entry.trigger)
        .put("expansion", entry.expansion)

    private fun decodeEntry(value: JSONObject) = TextShortcut(
        id = UUID.fromString(value.getString("id")),
        trigger = value.getString("trigger"),
        expansion = value.getString("expansion"),
    )
}

package app.funput.funput.keyboard.ui.kaomoji.persistence

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

internal class KaomojiRecentsStore(context: Context) {
    private val preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)

    fun texts(): List<String> = decode(preferences.getString(Key, null))

    fun record(text: String) {
        val updated = (listOf(text) + texts()).distinct().take(Limit)
        preferences.edit { putString(Key, encode(updated)) }
    }

    companion object {
        const val Limit = 30
        private const val FileName = "funput_kaomoji_recents"
        private const val Key = "recent_kaomoji"

        internal fun decode(value: String?): List<String> = runCatching {
            val array = JSONArray(value ?: "[]")
            buildList(array.length()) {
                repeat(array.length()) { index ->
                    array.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }.distinct().take(Limit)
        }.getOrDefault(emptyList())

        internal fun encode(values: List<String>): String = JSONArray(values).toString()
    }
}

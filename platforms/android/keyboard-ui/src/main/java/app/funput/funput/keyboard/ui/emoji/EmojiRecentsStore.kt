package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import androidx.core.content.edit

internal class EmojiRecentsStore(context: Context) {
    private val preferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE)

    fun glyphs(): List<String> = decode(preferences.getString(Key, null))

    fun record(glyph: String) {
        val updated = (listOf(glyph) + glyphs()).distinct().take(Limit)
        preferences.edit { putString(Key, updated.joinToString(",")) }
    }

    companion object {
        const val FileName = "androidx.emoji2.emojipicker.preferences"
        const val Key = "pref_key_recent_emoji"
        const val Limit = 30

        internal fun shouldRecord(sourceCategory: EmojiCategory): Boolean =
            sourceCategory != EmojiCategory.RECENT

        internal fun decode(value: String?): List<String> = value.orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(Limit)
    }
}

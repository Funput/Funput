package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardTheme
import org.json.JSONObject

/**
 * Serializes a bare [KeyboardTheme] token set as JSON text.
 *
 * The descriptor codec covers stored themes; this exists for callers that need to carry an
 * in-flight theme across a process death, such as the editor's saved instance state. It shares
 * the token table with the store, so a token can never round-trip in one place and not the other.
 */
object KeyboardThemeJson {
    fun encode(theme: KeyboardTheme): String = KeyboardThemeTokenJson.encode(theme).toString()

    fun decode(text: String): KeyboardTheme = KeyboardThemeTokenJson.decode(JSONObject(text))
}

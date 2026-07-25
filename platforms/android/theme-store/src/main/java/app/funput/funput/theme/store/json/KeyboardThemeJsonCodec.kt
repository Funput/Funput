package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import org.json.JSONObject

/**
 * Serializes a [KeyboardThemeDescriptor] as JSON.
 *
 * This is the format custom themes are stored in, and the format a downloadable theme pack will
 * arrive in. [SchemaVersion] is written so a future reader can branch on it; readers today treat
 * an absent value as version 1 and rely on per-token fallbacks instead of branching.
 */
internal object KeyboardThemeJsonCodec {
    const val SchemaVersion = 1

    fun encode(descriptor: KeyboardThemeDescriptor): String = JSONObject().apply {
        put("schemaVersion", SchemaVersion)
        put("id", descriptor.id.value)
        put("version", descriptor.version)
        put("name", descriptor.name)
        put("author", descriptor.author)
        put("origin", descriptor.origin.name)
        descriptor.baseThemeId?.let { id -> put("baseThemeId", id.value) }
        descriptor.backgroundImage?.let { background ->
            put(
                "backgroundImage",
                JSONObject()
                    .put("source", background.source)
                    .put("opacity", background.opacity.toDouble()),
            )
        }
        put("theme", KeyboardThemeTokenJson.encode(descriptor.theme))
    }.toString(IndentSpaces)

    fun decode(text: String): KeyboardThemeDescriptor {
        val json = JSONObject(text)
        return KeyboardThemeDescriptor(
            id = KeyboardThemeId.of(json.getString("id")),
            version = json.getInt("version"),
            name = json.getString("name"),
            author = json.getString("author"),
            origin = KeyboardThemeOrigin.valueOf(json.getString("origin")),
            baseThemeId = json.optString("baseThemeId")
                .takeIf { value -> value.isNotEmpty() }
                ?.let(KeyboardThemeId::of),
            backgroundImage = json.optJSONObject("backgroundImage")?.backgroundImage(),
            theme = KeyboardThemeTokenJson.decode(json.getJSONObject("theme")),
        )
    }

    private fun JSONObject.backgroundImage() = KeyboardThemeBackgroundImage(
        source = getString("source"),
        opacity = getDouble("opacity").toFloat(),
    )

    private const val IndentSpaces = 2
}

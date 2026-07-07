package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeId
import java.text.Normalizer
import java.util.Locale

/** Generates stable custom theme identifiers from user-facing names. */
class CustomThemeIdGenerator(
    private val namespace: String = "custom",
) {
    fun generate(name: String, existingIds: Set<KeyboardThemeId>): KeyboardThemeId {
        val baseValue = "$namespace.${name.slug()}"
        var candidate = KeyboardThemeId.of(baseValue)
        var suffix = 2

        while (candidate in existingIds) {
            candidate = KeyboardThemeId.of("$baseValue-$suffix")
            suffix += 1
        }
        return candidate
    }

    private fun String.slug(): String {
        val ascii = Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.US)
        return ascii
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(MaxSlugLength)
            .trim('-')
            .ifBlank { "theme" }
    }

    private companion object {
        const val MaxSlugLength = 72
    }
}

package app.funput.funput.theme.store

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.store.json.KeyboardThemeJsonCodec
import java.io.File

/**
 * File-backed store for user-created keyboard themes, one file per theme.
 *
 * Themes are written as JSON. Themes saved by an earlier build are `.properties` files; those are
 * still read, and the next save of that theme rewrites it as JSON and removes the old file, so the
 * migration happens in place without a separate pass and without losing anything on the way.
 */
class CustomThemeFileStore(
    private val directory: File,
) : CustomKeyboardThemeStore {
    override fun loadThemes(): List<KeyboardThemeDescriptor> {
        val files = directory.listFiles { file ->
            file.isFile && file.extension in ReadableExtensions
        } ?: return emptyList()

        return files
            // A theme mid-migration has both files. Read JSON first so it is the one kept.
            .sortedBy { file -> if (file.extension == JsonExtension) 0 else 1 }
            .mapNotNull(::readThemeOrNull)
            .filter { theme -> theme.origin == KeyboardThemeOrigin.CUSTOM }
            .distinctBy { theme -> theme.id }
            .sortedWith(compareBy({ theme -> theme.name.lowercase() }, { theme -> theme.id.value }))
    }

    override fun upsertTheme(theme: KeyboardThemeDescriptor) {
        require(theme.origin == KeyboardThemeOrigin.CUSTOM) {
            "Only custom themes can be stored in the custom theme store"
        }
        ensureDirectoryExists()

        val destination = themeFile(theme.id, JsonExtension)
        val temporary = File.createTempFile(destination.nameWithoutExtension, ".tmp", directory)
        temporary.writeText(KeyboardThemeJsonCodec.encode(theme))
        replaceFile(temporary, destination)
        themeFile(theme.id, LegacyExtension).delete()
    }

    override fun deleteTheme(id: KeyboardThemeId): Boolean {
        val removedJson = themeFile(id, JsonExtension).delete()
        val removedLegacy = themeFile(id, LegacyExtension).delete()
        return removedJson || removedLegacy
    }

    private fun readThemeOrNull(file: File): KeyboardThemeDescriptor? =
        runCatching {
            if (file.extension == JsonExtension) {
                KeyboardThemeJsonCodec.decode(file.readText())
            } else {
                file.inputStream().use(KeyboardThemeDescriptorPropertiesCodec::decode)
            }
        }.getOrNull()

    private fun ensureDirectoryExists() {
        require(directory.exists() || directory.mkdirs()) {
            "Unable to create custom theme directory: ${directory.absolutePath}"
        }
    }

    private fun themeFile(id: KeyboardThemeId, extension: String): File =
        File(directory, "${id.value}.$extension")

    private fun replaceFile(source: File, destination: File) {
        if (source.renameTo(destination)) return

        source.copyTo(destination, overwrite = true)
        source.delete()
    }

    private companion object {
        const val JsonExtension = "json"
        const val LegacyExtension = "properties"

        val ReadableExtensions = setOf(JsonExtension, LegacyExtension)
    }
}

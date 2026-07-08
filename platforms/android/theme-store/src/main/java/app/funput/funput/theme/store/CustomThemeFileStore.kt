package app.funput.funput.theme.store

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import java.io.File

/** File-backed store for user-created keyboard themes. */
class CustomThemeFileStore(
    private val directory: File,
) : CustomKeyboardThemeStore {
    private val codec = KeyboardThemeDescriptorPropertiesCodec

    override fun loadThemes(): List<KeyboardThemeDescriptor> {
        val files = directory.listFiles { file ->
            file.isFile && file.extension == FileExtension
        } ?: return emptyList()

        return files
            .mapNotNull(::readThemeOrNull)
            .filter { theme -> theme.origin == KeyboardThemeOrigin.CUSTOM }
            .sortedWith(compareBy({ theme -> theme.name.lowercase() }, { theme -> theme.id.value }))
    }

    override fun upsertTheme(theme: KeyboardThemeDescriptor) {
        require(theme.origin == KeyboardThemeOrigin.CUSTOM) {
            "Only custom themes can be stored in the custom theme store"
        }
        ensureDirectoryExists()

        val destination = themeFile(theme.id)
        val temporary = File.createTempFile(destination.nameWithoutExtension, ".tmp", directory)
        temporary.outputStream().use { output -> codec.encode(theme, output) }
        replaceFile(temporary, destination)
    }

    override fun deleteTheme(id: KeyboardThemeId): Boolean = themeFile(id).delete()

    private fun readThemeOrNull(file: File): KeyboardThemeDescriptor? =
        runCatching {
            file.inputStream().use(codec::decode)
        }.getOrNull()

    private fun ensureDirectoryExists() {
        require(directory.exists() || directory.mkdirs()) {
            "Unable to create custom theme directory: ${directory.absolutePath}"
        }
    }

    private fun themeFile(id: KeyboardThemeId): File = File(directory, "${id.value}.$FileExtension")

    private fun replaceFile(source: File, destination: File) {
        if (source.renameTo(destination)) return

        source.copyTo(destination, overwrite = true)
        source.delete()
    }

    private companion object {
        const val FileExtension = "properties"
    }
}

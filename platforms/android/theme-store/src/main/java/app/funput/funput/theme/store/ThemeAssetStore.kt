package app.funput.funput.theme.store

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Holds background images inside the app's own storage.
 *
 * A picked photo arrives as a content URI, which is the wrong thing to persist in a theme: the
 * grant can be revoked, the user can delete the photo, and the URI means nothing to another
 * device. Copying the bytes makes a theme self-contained, which is also what a theme that can be
 * exported or downloaded later will need.
 */
class ThemeAssetStore(private val directory: File) {
    /** Copies [uri] in and returns the stored asset's absolute path, or null if it cannot be read. */
    fun store(context: Context, uri: Uri): String? {
        if (!(directory.exists() || directory.mkdirs())) return null
        val destination = File(directory, "${UUID.randomUUID()}.img")
        val temporary = File(directory, ".tmp-${destination.name}")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use(input::copyTo)
            } ?: error("Unable to open $uri")
            check(temporary.renameTo(destination)) { "Unable to finalize ${destination.name}" }
            destination.absolutePath
        }.getOrElse {
            temporary.delete()
            null
        }
    }

    /**
     * Deletes assets no theme refers to any more.
     *
     * Called after a theme is saved or deleted; a theme that swapped its image would otherwise
     * leave the old file behind forever.
     */
    fun removeUnreferenced(referencedPaths: Set<String>) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in referencedPaths) file.delete()
        }
    }
}

fun Context.themeAssetStore(): ThemeAssetStore =
    ThemeAssetStore(File(applicationContext.filesDir, "keyboard-themes/assets"))

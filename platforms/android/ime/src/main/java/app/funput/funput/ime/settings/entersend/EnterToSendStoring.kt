package app.funput.funput.ime.settings.entersend

import kotlinx.coroutines.flow.Flow

/** The apps whose Enter key sends, shared by Settings and the IME. */
interface EnterToSendStoring {
    /** The chosen package names; never contains an invalid name. */
    val packages: Flow<Set<String>>

    /** Adds [packageName] to the latest persisted list; invalid names are ignored. */
    suspend fun add(packageName: String)

    /** Removes [packageName] from the latest persisted list. */
    suspend fun remove(packageName: String)
}

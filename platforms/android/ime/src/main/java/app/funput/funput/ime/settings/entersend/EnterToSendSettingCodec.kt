package app.funput.funput.ime.settings.entersend

/**
 * Stores the chosen apps as one canonical string: sorted package names, one per line. A single
 * string keeps every edit a read-modify-write of one key, like the extra onsets selection.
 */
internal object EnterToSendSettingCodec {
    /** Reads a stored value; an absent key is the default, an empty list. */
    fun decode(value: String?): Set<String> =
        value.orEmpty().lineSequence().map(String::trim).filter(::isPackageName).toSortedSet()

    fun encode(packages: Set<String>): String =
        packages.filter(::isPackageName).sorted().joinToString(Separator)

    /** Android package names: two or more dot-separated Java identifiers. */
    fun isPackageName(value: String): Boolean = PackageName.matches(value)

    private const val Separator = "\n"
    private val PackageName = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
}

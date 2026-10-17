package app.funput.funput.ime.editing.correction

/**
 * The engine counts in Unicode scalars and Android edits in UTF-16 units. These cut our own
 * strings by scalar; lengths handed to the editor always come from the resulting strings.
 */
internal fun String.scalarCount(): Int = codePointCount(0, length)

/** This string without its last [count] scalars; empty when it has fewer. */
internal fun String.dropLastScalars(count: Int): String {
    val scalars = scalarCount()
    if (count >= scalars) return ""
    return substring(0, offsetByCodePoints(0, scalars - count))
}

/** The last [count] scalars of this string; the whole string when it has fewer. */
internal fun String.lastScalars(count: Int): String {
    val scalars = scalarCount()
    if (count >= scalars) return this
    return substring(offsetByCodePoints(0, scalars - count))
}

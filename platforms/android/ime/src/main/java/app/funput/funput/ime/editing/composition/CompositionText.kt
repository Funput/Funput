package app.funput.funput.ime.editing.composition

internal const val CursorAfterText = 1

internal fun String.singleCodePointOrNull(): Int? {
    val first = codePointAt(0)
    return first.takeIf { Character.charCount(it) == length }
}

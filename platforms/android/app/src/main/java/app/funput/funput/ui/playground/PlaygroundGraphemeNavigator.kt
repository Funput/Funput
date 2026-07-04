package app.funput.funput.ui.playground

import java.text.BreakIterator
import java.util.Locale

/** Character boundaries with emoji-sequence handling missing from some Java runtimes. */
internal object PlaygroundGraphemeNavigator {
    fun previous(text: String, offset: Int): Int {
        requireOffset(text, offset)
        if (offset == 0) return 0
        if (isRegionalIndicator(text.codePointBefore(offset))) {
            val count = regionalIndicatorsBefore(text, offset)
            val codePointsToRemove = if (count % 2 == 0) 2 else 1
            return text.offsetByCodePoints(offset, -codePointsToRemove)
        }

        var start = expandExtendersBackward(text, boundaryBefore(text, offset))
        while (codePointBeforeOrNull(text, start) == ZeroWidthJoiner) {
            val joinerStart = previousCodePointOffset(text, start)
            start = expandExtendersBackward(text, boundaryBefore(text, joinerStart))
        }
        return start
    }

    fun next(text: String, offset: Int): Int {
        requireOffset(text, offset)
        if (offset == text.length) return text.length
        if (isRegionalIndicator(text.codePointAt(offset))) {
            val firstIndicatorEnd = text.offsetByCodePoints(offset, 1)
            val hasFollowingIndicator = firstIndicatorEnd < text.length &&
                isRegionalIndicator(text.codePointAt(firstIndicatorEnd))
            val indicatorsBefore = regionalIndicatorsBefore(text, offset)
            val pairSize = if (indicatorsBefore % 2 == 0 && hasFollowingIndicator) 2 else 1
            return text.offsetByCodePoints(offset, pairSize)
        }

        var end = expandExtendersForward(text, boundaryAfter(text, offset))
        while (end < text.length && text.codePointAt(end) == ZeroWidthJoiner) {
            val afterJoiner = end + Character.charCount(ZeroWidthJoiner)
            end = expandExtendersForward(text, boundaryAfter(text, afterJoiner))
        }
        return end
    }

    fun floor(text: String, offset: Int): Int {
        val target = offset.coerceIn(0, text.length)
        var boundary = 0
        while (boundary < target) {
            val next = next(text, boundary)
            if (next > target) break
            boundary = next
        }
        return boundary
    }

    private fun expandExtendersBackward(text: String, initial: Int): Int {
        var start = initial
        while (start > 0 && isEmojiExtender(text.codePointAt(start))) {
            start = boundaryBefore(text, start)
        }
        return start
    }

    private fun expandExtendersForward(text: String, initial: Int): Int {
        var end = initial
        while (end < text.length && isEmojiExtender(text.codePointAt(end))) {
            end = boundaryAfter(text, end)
        }
        return end
    }

    private fun boundaryBefore(text: String, offset: Int): Int = iterator(text)
        .preceding(offset)
        .takeUnless { it == BreakIterator.DONE }
        ?: 0

    private fun boundaryAfter(text: String, offset: Int): Int = iterator(text)
        .following(offset)
        .takeUnless { it == BreakIterator.DONE }
        ?: text.length

    private fun iterator(text: String) = BreakIterator.getCharacterInstance(Locale.ROOT).apply {
        setText(text)
    }

    private fun regionalIndicatorsBefore(text: String, offset: Int): Int {
        var count = 0
        var current = offset
        while (current > 0 && isRegionalIndicator(text.codePointBefore(current))) {
            current = previousCodePointOffset(text, current)
            count++
        }
        return count
    }

    private fun previousCodePointOffset(text: String, offset: Int): Int =
        offset - Character.charCount(text.codePointBefore(offset))

    private fun codePointBeforeOrNull(text: String, offset: Int): Int? =
        if (offset > 0) text.codePointBefore(offset) else null

    private fun isEmojiExtender(codePoint: Int): Boolean =
        codePoint in EmojiModifierRange || codePoint in VariationSelectorRange ||
            codePoint in SupplementaryVariationSelectorRange || codePoint in EmojiTagRange

    private fun isRegionalIndicator(codePoint: Int) = codePoint in RegionalIndicatorRange

    private fun requireOffset(text: String, offset: Int) {
        require(offset in 0..text.length) { "Offset must be within the text" }
    }

    private val EmojiModifierRange = 0x1F3FB..0x1F3FF
    private val VariationSelectorRange = 0xFE00..0xFE0F
    private val SupplementaryVariationSelectorRange = 0xE0100..0xE01EF
    private val EmojiTagRange = 0xE0020..0xE007F
    private val RegionalIndicatorRange = 0x1F1E6..0x1F1FF
    private const val ZeroWidthJoiner = 0x200D
}

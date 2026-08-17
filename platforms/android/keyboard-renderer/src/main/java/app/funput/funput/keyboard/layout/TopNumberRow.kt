package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardRow

internal enum class TopNumberRowMode {
    VNI_MODIFIERS,
    PLAIN_CHARACTER,
    PLAIN_PUNCTUATION,
}

internal fun topNumberRowForLetters(inputMethod: KeyboardInputMethod): KeyboardRow =
    topNumberRowFor(inputMethod, "letters-${inputMethod.name.lowercase()}", composesVietnamese = true)

/**
 * The digits row for [inputMethod], carrying VNI tone hints only where tones actually apply.
 *
 * VNI places tones with the digits, so the hints belong on any editor that composes Vietnamese —
 * a browser address bar included. On an editor that does not compose (email, URL), the same hints
 * would advertise an effect the keys no longer have, so the row stays plain.
 */
internal fun topNumberRowFor(
    inputMethod: KeyboardInputMethod,
    pageId: String,
    composesVietnamese: Boolean,
): KeyboardRow {
    val mode = if (!inputMethod.isTelexFamily && composesVietnamese) {
        TopNumberRowMode.VNI_MODIFIERS
    } else {
        TopNumberRowMode.PLAIN_CHARACTER
    }
    return topNumberRow(mode, pageId)
}

internal fun topNumberRow(mode: TopNumberRowMode, pageId: String): KeyboardRow {
    val hints = listOf("´", "`", "̉", "˜", "̣", "ˆ", "+", "˘", "đ", "×")
    val descriptions = listOf(
        "Sắc tone", "Huyền tone", "Hỏi tone", "Ngã tone", "Nặng tone",
        "Circumflex modifier", "Horn modifier", "Breve modifier",
        "D stroke modifier", "Remove tone",
    )
    return KeyboardRow(
        keys = (0..9).map { index ->
            val digit = if (index == 9) 0 else index + 1
            val label = digit.toString()
            when (mode) {
                TopNumberRowMode.VNI_MODIFIERS -> KeySpec(
                    id = "vni-$pageId-$digit",
                    label = label,
                    secondaryLabel = hints[index],
                    role = KeyRole.VNI_MODIFIER,
                    accessibilityLabel = descriptions[index],
                )
                TopNumberRowMode.PLAIN_CHARACTER -> KeySpec(
                    id = "digit-$pageId-$digit",
                    label = label,
                    role = KeyRole.CHARACTER,
                    accessibilityLabel = label,
                )
                TopNumberRowMode.PLAIN_PUNCTUATION -> KeySpec(
                    id = "digit-$pageId-$digit",
                    label = label,
                    role = KeyRole.PUNCTUATION,
                    accessibilityLabel = label,
                )
            }
        },
    )
}

package app.funput.funput.keyboard.layout.panels

import app.funput.funput.keyboard.layout.letters.qwertyLayout
import app.funput.funput.keyboard.layout.letters.specialKey
import app.funput.funput.keyboard.layout.rows.topNumberRowFor
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

/**
 * The letters page for a search field that lives inside a keyboard panel (emoji search
 * today), shaped by the method its field composes with.
 *
 * VNI brings its modifier digit row and Telex its key hints; a field that keeps keys
 * literal (`null`, English) gets plain letters. There is no suggestion bar and no
 * symbols page: the bottom row leaves the search, spaces or closes the keyboard.
 */
object PanelSearchKeyboardLayouts {
    fun letters(inputMethod: KeyboardInputMethod?, spaceLabel: String): KeyboardLayout {
        val pageId = "panel-search-${inputMethod?.name?.lowercase() ?: "plain"}"
        val leadingRows = if (inputMethod == KeyboardInputMethod.VNI) {
            listOf(topNumberRowFor(inputMethod, pageId, composesVietnamese = true))
        } else {
            emptyList()
        }
        return qwertyLayout(
            id = pageId,
            inputMethod = inputMethod ?: KeyboardInputMethod.TELEX,
            leadingRows = leadingRows,
            actionKeys = listOf(
                specialKey("emoji", "☺", KeyRole.EMOJI, 1.7f, "Thoát tìm kiếm"),
                KeySpec(
                    id = "space",
                    label = spaceLabel,
                    role = KeyRole.SPACE,
                    widthWeight = 5.8f,
                    accessibilityLabel = "Dấu cách",
                    spaceLabelOverride = spaceLabel,
                ),
                specialKey("done", "Xong", KeyRole.ENTER, 1.7f),
            ),
            showSuggestionBar = false,
            showsTelexHints = inputMethod?.isTelexFamily == true,
        )
    }
}

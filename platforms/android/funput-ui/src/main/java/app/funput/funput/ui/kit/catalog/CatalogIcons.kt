package app.funput.funput.ui.kit.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi

/** One row per group colour, then every icon in the set, so a new or odd glyph stands out. */
private val TintSamples = listOf(
    Triple("Gõ tiếng Việt", FunputIcons.Keyboard, FunputTint.ORANGE),
    Triple("Bố cục", FunputIcons.KeySize, FunputTint.BLUE),
    Triple("Thông minh", FunputIcons.Suggestions, FunputTint.PURPLE),
    Triple("Âm thanh & rung", FunputIcons.Haptics, FunputTint.PINK),
    Triple("Clipboard", FunputIcons.Clipboard, FunputTint.GREEN),
    Triple("Bàn phím vật lý", FunputIcons.HardwareKeyboard, FunputTint.GRAY),
    Triple("Dữ liệu", FunputIcons.Delete, FunputTint.RED),
)

private val AllIcons = with(FunputIcons) {
    listOf(
        Settings, SettingsSelected, Appearance, AppearanceSelected, About, AboutSelected, Back, Forward,
        Check, Add, Keyboard, ToneMarks, Shortcuts, NumberRow, KeySize, Placement, OneHanded, Restore,
        SpellCheck, Capitalize, Suggestions, Gestures, ReturnToLetters, Haptics, Sound, Clipboard,
        ClipboardExpiry, HardwareKeyboard, Delete, Clear,
    )
}

/** The catalog's icon section: group colours as they appear in rows, and the whole set. */
@Composable
internal fun CatalogIcons() {
    FunputSection(title = "Icon", footer = "Phosphor (MIT). Mỗi nhóm cài đặt một màu.") {
        TintSamples.forEachIndexed { index, (title, icon, tint) ->
            if (index > 0) FunputDivider(startInset = 62.dp)
            FunputRow(title = title, icon = icon, tint = tint)
        }
        FunputDivider()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
            modifier = Modifier.padding(FunputUi.spacing.cardPadding),
        ) {
            AllIcons.forEach { icon ->
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(FunputUi.colors.label),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

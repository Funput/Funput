package app.funput.funput.ui.kit.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.ChoiceRow
import app.funput.funput.ui.kit.theme.FunputUi

/** The third tab: segmented control, every button style, and the sheet and dialog, live. */
internal fun LazyListScope.controlSections() {
    item(key = "segmented") {
        var tone by rememberSaveable { mutableIntStateOf(0) }
        FunputSection(title = "Segmented", footer = "Cho vài lựa chọn ngắn; nhiều hơn thì dùng sheet.") {
            FunputSegmented(
                options = listOf("Truyền thống", "Hiện đại"),
                selectedIndex = tone,
                onSelect = { tone = it },
                modifier = Modifier.padding(FunputUi.spacing.medium),
            )
        }
    }
    item(key = "buttons") {
        Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(FunputUi.spacing.medium)) {
            FunputButtonStyle.entries.forEach { style ->
                FunputButton(
                    text = style.name.lowercase(),
                    onClick = {},
                    style = style,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FunputButton(text = "Bị tắt", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
    item(key = "overlays") {
        var sheet by rememberSaveable { mutableStateOf(false) }
        var dialog by rememberSaveable { mutableStateOf(false) }
        var method by rememberSaveable { mutableIntStateOf(0) }
        Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(FunputUi.spacing.medium)) {
            val secondary = FunputButtonStyle.SECONDARY
            FunputButton("Mở sheet", { sheet = true }, Modifier.fillMaxWidth(), secondary)
            FunputButton("Mở dialog", { dialog = true }, Modifier.fillMaxWidth(), secondary)
        }
        if (sheet) {
            FunputSheet(onDismiss = { sheet = false }, title = "Kiểu gõ") {
                FunputSection(title = null) {
                    listOf("Telex", "VNI", "Telex nâng cao").forEachIndexed { index, option ->
                        if (index > 0) FunputDivider()
                        ChoiceRow(title = option, selected = method == index, onSelect = { method = index })
                    }
                }
            }
        }
        if (dialog) {
            FunputDialog(
                title = "Xoá từ đã học?",
                message = "Funput sẽ quên mọi từ bạn đã gõ. Không thể hoàn tác.",
                confirmLabel = "Xoá",
                onConfirm = { dialog = false },
                onDismiss = { dialog = false },
                dismissLabel = "Huỷ",
                destructive = true,
            )
        }
    }
}

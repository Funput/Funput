package app.funput.funput.ui.kit.catalog

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.ChoiceRow
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.SliderRow
import app.funput.funput.ui.kit.rows.ToggleRow

/** The second tab: every row kind, live, with a value long enough to wrap. */
internal fun LazyListScope.rowSections() {
    item(key = "links") {
        val footer = "Giá trị dài tự xuống dưới tiêu đề khi không đủ chỗ."
        FunputSection(title = "Liên kết", footer = footer) {
            LinkRow(title = "Kiểu gõ", value = "VNI", onClick = {})
            FunputDivider()
            LinkRow(title = "Bảng mã khi dán", value = "Unicode dựng sẵn (khuyên dùng)", onClick = {})
            FunputDivider()
            LinkRow(title = "Gõ tắt", summary = "Thay chữ tắt bằng nội dung dài hơn", onClick = {})
        }
    }
    item(key = "toggles") {
        var restore by rememberSaveable { mutableStateOf(true) }
        var haptics by rememberSaveable { mutableStateOf(false) }
        FunputSection(title = "Công tắc") {
            ToggleRow(
                title = "Khôi phục từ",
                summary = "Gõ sai thì trả lại chữ gốc",
                checked = restore,
                onCheckedChange = { restore = it },
            )
            FunputDivider()
            ToggleRow(title = "Rung khi gõ", checked = haptics, onCheckedChange = { haptics = it })
            FunputDivider()
            ToggleRow(title = "Âm thanh (bị tắt)", checked = false, onCheckedChange = {}, enabled = false)
        }
    }
    item(key = "choices") {
        var choice by rememberSaveable { mutableIntStateOf(0) }
        var height by rememberSaveable { mutableFloatStateOf(0.6f) }
        FunputSection(title = "Lựa chọn") {
            listOf("Chuẩn", "Nâng cao", "Một tay").forEachIndexed { index, option ->
                if (index > 0) FunputDivider()
                ChoiceRow(title = option, selected = choice == index, onSelect = { choice = index })
            }
            FunputDivider()
            SliderRow(
                title = "Độ cao phím",
                value = height,
                onValueChange = { height = it },
                valueLabel = "${(height * 100).toInt()}%",
            )
        }
    }
}

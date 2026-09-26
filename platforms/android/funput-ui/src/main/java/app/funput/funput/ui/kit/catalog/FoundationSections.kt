package app.funput.funput.ui.kit.catalog

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.glass.currentGlassTier
import app.funput.funput.ui.kit.theme.FunputUi

/** The first tab: glass tier switch, colourful content to judge glass over, type and colours. */
internal fun LazyListScope.foundationSections(tierIndex: Int, onTierSelected: (Int) -> Unit) {
    item(key = "glass") {
        FunputSection(
            title = "Kính",
            footer = "Đang vẽ: ${currentGlassTier().name} · Android API ${Build.VERSION.SDK_INT}. " +
                "Cuộn nội dung dưới thanh tiêu đề và thanh tab để so hai tầng.",
        ) {
            FunputSegmented(
                options = listOf("Theo máy", "Kính", "Mờ đục"),
                selectedIndex = tierIndex,
                onSelect = onTierSelected,
                modifier = Modifier.padding(FunputUi.spacing.medium),
            )
        }
    }
    item(key = "banners") { CatalogBanners() }
    item(key = "type") {
        val type = FunputUi.typography
        val label = FunputUi.colors.label
        FunputSection(title = "Chữ") {
            Column(Modifier.padding(FunputUi.spacing.cardPadding), Arrangement.spacedBy(FunputUi.spacing.small)) {
                listOf(
                    type.display to "Ấy ơi, gõ tiếng Việt",
                    type.title to "Kiểu gõ và bố cục",
                    type.headline to "Khôi phục từ khi gõ sai",
                    type.body to "Nguyễn Thị Ỡ viết ế, ự, ỡ, ẫ.",
                    type.label to "CÀI ĐẶT · GIAO DIỆN",
                    type.caption to "Mục đã ghim không bao giờ tự xoá.",
                ).forEach { (style, text) -> BasicText(text, style = style.copy(color = label)) }
            }
        }
    }
    item(key = "colors") {
        val colors = FunputUi.colors
        FunputSection(title = "Màu") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.small),
                modifier = Modifier.padding(FunputUi.spacing.cardPadding),
            ) {
                listOf(colors.accent, colors.success, colors.destructive, colors.label, colors.secondaryLabel)
                    .forEach { Swatch(it) }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(
        Modifier
            .size(40.dp)
            .clip(FunputUi.shapes.iconTile)
            .background(color)
            .border(FunputUi.spacing.cardStrokeWidth, FunputUi.colors.cardStroke, FunputUi.shapes.iconTile),
    )
}

/** Vivid gradients, so blur and refraction have something to show when they pass over it. */
@Composable
private fun CatalogBanners() {
    val gradients = listOf(
        listOf(Color(0xFFFF9500), Color(0xFFFF2D55)),
        listOf(Color(0xFFAF52DE), Color(0xFF007AFF)),
        listOf(Color(0xFF34C759), Color(0xFF30B0C7)),
    )
    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(FunputUi.spacing.large)) {
        gradients.forEachIndexed { index, pair -> CatalogBanner("Chủ đề ${index + 1}", pair) }
    }
}

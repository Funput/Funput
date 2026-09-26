package app.funput.funput.ui.kit.layout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.glass.GlassTier
import app.funput.funput.ui.kit.glass.LocalGlassTier
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.theme.FunputUiTheme

/** The three tabs the app has, used by the layout tests. */
internal val SampleTabs = listOf(FunputTab("Cài đặt"), FunputTab("Giao diện"), FunputTab("Giới thiệu"))

/**
 * A settings-like screen built only from P1.2 components: sections of text lines inside cards,
 * under a large title, with the floating tab bar. Glass is pinned to [GlassTier.SOLID] because
 * JVM rendering cannot run the blur and refraction shaders.
 */
@Composable
internal fun SampleScreen(isDark: Boolean, onBack: (() -> Unit)? = null, onSelectTab: (Int) -> Unit = {}) {
    FunputUiTheme(isDark = isDark) {
        CompositionLocalProvider(LocalGlassTier provides GlassTier.SOLID) {
            FunputScreen(
                title = "Cài đặt",
                onBack = onBack,
                tabBar = { FunputTabBar(SampleTabs, selectedIndex = 0, onSelect = onSelectTab) },
            ) {
                listOf("Gõ tiếng Việt", "Bố cục bàn phím", "Thông minh", "Clipboard").forEach { group ->
                    item(key = group) {
                        FunputSection(title = group, footer = "Ghi chú ngắn giải thích nhóm $group.") {
                            listOf("Kiểu gõ", "Kiểu đặt dấu", "Gõ tắt").forEachIndexed { index, line ->
                                if (index > 0) FunputDivider()
                                SampleLine(line)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleLine(text: String) {
    BasicText(
        text = text,
        style = FunputUi.typography.body.copy(color = FunputUi.colors.label),
        modifier = Modifier.fillMaxWidth().padding(FunputUi.spacing.cardPadding),
    )
}

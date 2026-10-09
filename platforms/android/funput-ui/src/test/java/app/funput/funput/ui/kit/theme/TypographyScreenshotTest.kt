package app.funput.funput.ui.kit.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.funput.funput.ui.kit.KIT_SCREENSHOT_ROOT
import app.funput.funput.uitesting.SCREENSHOT_SDK
import app.funput.funput.uitesting.ScreenshotDevices
import app.funput.funput.uitesting.ScreenshotVariant
import app.funput.funput.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The whole type scale set in Vietnamese with the tallest stacked marks, so a line height that
 * clips "Ấ" or "ỡ", or a font that failed to load, shows up in the image.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = ScreenshotDevices.PHONE)
class TypographyScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun specimen() = compose.captureScreen("typography", variant, root = KIT_SCREENSHOT_ROOT) {
        FunputUiTheme(isDark = variant.isDark) { Specimen() }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}

@Composable
private fun Specimen() {
    val type = FunputUi.typography
    val colors = FunputUi.colors
    val samples = listOf(
        type.display to "Ấy ơi, gõ tiếng Việt",
        type.title to "Kiểu gõ và bố cục",
        type.headline to "Khôi phục từ khi gõ sai",
        type.body to "Nguyễn Thị Ỡ viết ế, ự, ỡ, ẫ rất đẹp.",
        type.label to "CÀI ĐẶT · GIAO DIỆN · GIỚI THIỆU",
        type.caption to "Mục đã ghim không bao giờ tự xoá.",
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
        modifier = Modifier.fillMaxWidth().background(colors.groupedBackground).padding(FunputUi.spacing.pageMargin),
    ) {
        samples.forEach { (style, text) -> BasicText(text, style = style.copy(color = colors.label)) }
        BasicText("Chữ phụ: tóm tắt và giá trị", style = type.body.copy(color = colors.secondaryLabel))
        BasicText("Màu nhấn Funput", style = type.headline.copy(color = colors.accent))
    }
}

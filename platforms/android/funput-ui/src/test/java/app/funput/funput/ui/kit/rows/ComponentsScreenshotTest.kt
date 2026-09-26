package app.funput.funput.ui.kit.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.theme.FunputUiTheme
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
 * Every row kind and control on one page, with real Vietnamese labels, including a value long
 * enough to wrap under its title at 130% font scale.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [SCREENSHOT_SDK], qualifiers = ScreenshotDevices.PHONE)
class ComponentsScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rows() = compose.captureScreen("kit/rows", variant) {
        FunputUiTheme(isDark = variant.isDark) { Page { RowsSamples() } }
    }

    @Test
    fun controls() = compose.captureScreen("kit/controls", variant) {
        FunputUiTheme(isDark = variant.isDark) { Page { ControlsSamples() } }
    }

    companion object {
        /** Every appearance × font-scale combination. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = ScreenshotVariant.parameters()
    }
}

/** Pages are kept shorter than the device, so no component is squeezed by the capture. */
@Composable
private fun Page(content: @Composable () -> Unit) {
    val spacing = FunputUi.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.section),
        modifier = Modifier.fillMaxWidth().background(FunputUi.colors.groupedBackground).padding(spacing.pageMargin),
    ) { content() }
}

@Composable
private fun RowsSamples() {
    FunputSection(title = "Gõ tiếng Việt", footer = "Kiểu đặt dấu áp dụng cho mọi kiểu gõ.") {
        LinkRow(title = "Kiểu gõ", value = "VNI", onClick = {})
        FunputDivider()
        LinkRow(title = "Bảng mã khi dán", value = "Unicode dựng sẵn (khuyên dùng)", onClick = {})
        FunputDivider()
        ToggleRow(
            title = "Khôi phục từ",
            summary = "Gõ sai thì trả lại chữ gốc",
            checked = true,
            onCheckedChange = {},
        )
        FunputDivider()
        ToggleRow(title = "Viết hoa đầu câu", checked = false, onCheckedChange = {}, enabled = false)
    }
}

@Composable
private fun ControlsSamples() {
    FunputSection(title = "Bố cục") {
        SliderRow(title = "Độ cao phím", value = 0.6f, onValueChange = {}, valueLabel = "60%")
        FunputDivider()
        ChoiceRow(title = "Chuẩn", selected = true, onSelect = {})
        FunputDivider()
        ChoiceRow(title = "Một tay", summary = "Thu hẹp về một bên", selected = false, onSelect = {})
    }
    FunputSegmented(listOf("Truyền thống", "Hiện đại"), selectedIndex = 0, onSelect = {})
    FunputButton("Bật Funput", onClick = {}, modifier = Modifier.fillMaxWidth())
    FunputButton(
        text = "Xoá từ đã học",
        onClick = {},
        style = FunputButtonStyle.DESTRUCTIVE,
        modifier = Modifier.fillMaxWidth(),
    )
}

package app.funput.funput.ui.settings.clipboard

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ClipboardSettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sectionShowsCurrentPreferencesAndDispatchesActions() {
        var enabled = true
        var openedExpiry = false
        setSection(
            enabled = enabled,
            expiry = ClipboardExpiry.DAY,
            onEnabledChanged = { enabled = it },
            onOpenExpiry = { openedExpiry = true },
        )

        compose.onNodeWithText("CLIPBOARD").assertExists()
        compose.onNodeWithText("1 ngày").assertExists()
        compose.onNodeWithText("Lưu lịch sử bảng nhớ tạm").performClick()
        compose.onNodeWithText("Tự xoá sau").performClick()

        compose.runOnIdle {
            assertFalse(enabled)
            assertTrue(openedExpiry)
        }
    }

    private fun setSection(
        enabled: Boolean = false,
        expiry: ClipboardExpiry = ClipboardExpiry.HOUR,
        onEnabledChanged: (Boolean) -> Unit = {},
        onOpenExpiry: () -> Unit = {},
    ) {
        compose.setContent {
            FunputTheme {
                ClipboardSettingsSection(
                    enabled = enabled,
                    expiry = expiry,
                    onEnabledChanged = onEnabledChanged,
                    onOpenExpiry = onOpenExpiry,
                )
            }
        }
    }
}

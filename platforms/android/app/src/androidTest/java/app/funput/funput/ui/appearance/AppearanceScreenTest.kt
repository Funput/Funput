package app.funput.funput.ui.appearance

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.KeyboardThemes
import app.funput.funput.ui.theme.FunputTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val customTheme = KeyboardThemeDescriptor(
        id = KeyboardThemeId.of("custom.ocean"),
        version = 1,
        name = "Ocean",
        author = "Me",
        origin = KeyboardThemeOrigin.CUSTOM,
        baseThemeId = KeyboardThemeId.Dark,
        theme = KeyboardThemes.Ink,
    )

    @Test
    fun showsSelectionAndDispatchesThemeActions() {
        var selectedThemeId = KeyboardThemeId.Dark
        var createRequested = false
        var editedThemeId: KeyboardThemeId? = null
        var deletedThemeId: KeyboardThemeId? = null

        compose.setContent {
            FunputTheme {
                AppearanceScreen(
                    testAppearanceState(
                        extraThemes = listOf(customTheme),
                        onThemeSelected = { selectedThemeId = it },
                        onCreateTheme = { createRequested = true },
                        onEditTheme = { editedThemeId = it },
                        onDeleteTheme = { deletedThemeId = it },
                    ),
                )
            }
        }

        compose.onNodeWithTag(CreateThemeTag).performClick()

        compose.onNodeWithTag(AppearanceListTag)
            .performScrollToNode(hasTestTag(customTheme.id.value))
        // Edit and delete live behind the card's overflow menu, which has to be opened each time:
        // choosing an item closes it.
        compose.onNodeWithContentDescription("Tuỳ chọn cho theme Ocean").performClick()
        compose.onNodeWithContentDescription("Sửa theme Ocean").performClick()
        compose.onNodeWithContentDescription("Tuỳ chọn cho theme Ocean").performClick()
        compose.onNodeWithContentDescription("Xóa theme Ocean").performClick()
        compose.onNodeWithText("Xóa theme").performClick()

        compose.onNodeWithTag(AppearanceListTag)
            .performScrollToNode(hasTestTag(KeyboardThemeId.Dark.value))
        compose.onNodeWithTag(KeyboardThemeId.Dark.value).assertIsSelected()
        compose.onNodeWithTag(AppearanceListTag)
            .performScrollToNode(hasTestTag(KeyboardThemeId.GlassLight.value))
        compose.onNodeWithTag(KeyboardThemeId.GlassLight.value)
            .assertIsNotSelected()
            .performClick()

        compose.runOnIdle {
            assertEquals(KeyboardThemeId.GlassLight, selectedThemeId)
            assertEquals(customTheme.id, editedThemeId)
            assertEquals(customTheme.id, deletedThemeId)
            assertTrue(createRequested)
        }
    }

    @Test
    fun slotChipsNameTheThemeInEachSlot() {
        var chosenSlot: app.funput.funput.ime.settings.KeyboardThemeSlot? = null

        compose.setContent {
            FunputTheme {
                AppearanceScreen(
                    testAppearanceState(
                        followsAppearance = true,
                        activeSlot = app.funput.funput.ime.settings.KeyboardThemeSlot.LIGHT,
                        onSlotSelected = { chosenSlot = it },
                    ),
                )
            }
        }

        // The chip has to say which theme it holds: tapping a card means a different thing
        // depending on which slot is active, so the slot cannot be the only thing on screen.
        compose.onNodeWithTag(AppearanceListTag)
            .performScrollToNode(hasText("Tối · Ink"))
        compose.onNodeWithText("Sáng · Paper").assertIsSelected()
        compose.onNode(hasText("Tối · Ink") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)

        compose.runOnIdle {
            assertEquals(app.funput.funput.ime.settings.KeyboardThemeSlot.DARK, chosenSlot)
        }
    }
}

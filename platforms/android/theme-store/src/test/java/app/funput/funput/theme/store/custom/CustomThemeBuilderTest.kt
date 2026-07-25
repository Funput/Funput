package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.KeyboardThemes
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CustomThemeBuilderTest {
    private val builder = CustomThemeBuilder()
    private val baseTheme = LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Dark)

    @Test
    fun buildCreatesCustomDescriptorFromDraftAndBaseTheme() {
        val draft = CustomThemeDraft(
            theme = KeyboardThemes.Paper,
            name = "  My   Ocean  ",
            author = "  Me  ",
            baseThemeId = baseTheme.id,
            backgroundImage = KeyboardThemeBackgroundImage("content://image/ocean", 0.48f),
        )

        val descriptor = builder.build(draft, baseTheme, existingThemeIds = setOf(baseTheme.id))

        assertEquals(KeyboardThemeId.of("custom.my-ocean"), descriptor.id)
        assertEquals("My Ocean", descriptor.name)
        assertEquals("Me", descriptor.author)
        assertEquals(KeyboardThemeOrigin.CUSTOM, descriptor.origin)
        assertEquals(baseTheme.id, descriptor.baseThemeId)
        assertEquals(draft.backgroundImage, descriptor.backgroundImage)
    }

    @Test
    fun buildStoresTheDraftTokensVerbatimRatherThanTheBaseTokens() {
        val draft = draft(theme = KeyboardThemes.Paper, baseThemeId = baseTheme.id)

        val descriptor = builder.build(draft, baseTheme, existingThemeIds = emptySet())

        assertEquals(KeyboardThemes.Paper, descriptor.theme)
    }

    @Test
    fun buildRejectsBlankThemeName() {
        val draft = draft(name = " ", baseThemeId = baseTheme.id)

        assertThrows(IllegalArgumentException::class.java) {
            builder.build(draft, baseTheme, existingThemeIds = emptySet())
        }
    }

    @Test
    fun buildRejectsMismatchedBaseTheme() {
        val draft = draft(baseThemeId = KeyboardThemeId.Light)

        assertThrows(IllegalArgumentException::class.java) {
            builder.build(draft, baseTheme, existingThemeIds = emptySet())
        }
    }

    @Test
    fun buildCanKeepExistingThemeIdForEdits() {
        val editedId = KeyboardThemeId.of("custom.my-ocean")
        val draft = draft(name = "Ocean 2", baseThemeId = baseTheme.id)

        val descriptor = builder.build(
            draft = draft,
            baseTheme = baseTheme,
            existingThemeIds = emptySet(),
            themeId = editedId,
        )

        assertEquals(editedId, descriptor.id)
        assertEquals("Ocean 2", descriptor.name)
    }

    private fun draft(
        theme: KeyboardTheme = KeyboardThemes.Ink,
        name: String = "Ocean",
        baseThemeId: KeyboardThemeId = KeyboardThemeId.Default,
    ) = CustomThemeDraft(theme = theme, name = name, baseThemeId = baseThemeId)
}

package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
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
            name = "  My   Ocean  ",
            author = "  Me  ",
            baseThemeId = baseTheme.id,
            backgroundImage = KeyboardThemeBackgroundImage("content://image/ocean", 0.48f),
            overrides = CustomThemeOverrides(accentColor = OceanAccent),
        )

        val descriptor = builder.build(draft, baseTheme, existingThemeIds = setOf(baseTheme.id))

        assertEquals(KeyboardThemeId.of("custom.my-ocean"), descriptor.id)
        assertEquals("My Ocean", descriptor.name)
        assertEquals("Me", descriptor.author)
        assertEquals(KeyboardThemeOrigin.CUSTOM, descriptor.origin)
        assertEquals(draft.backgroundImage, descriptor.backgroundImage)
        assertEquals(OceanAccent, descriptor.theme.accentColor)
        assertEquals(baseTheme.theme.keyColor, descriptor.theme.keyColor)
    }

    @Test
    fun buildRejectsBlankThemeName() {
        val draft = CustomThemeDraft(name = " ", baseThemeId = baseTheme.id)

        assertThrows(IllegalArgumentException::class.java) {
            builder.build(draft, baseTheme, existingThemeIds = emptySet())
        }
    }

    @Test
    fun buildRejectsMismatchedBaseTheme() {
        val draft = CustomThemeDraft(name = "Ocean", baseThemeId = KeyboardThemeId.Light)

        assertThrows(IllegalArgumentException::class.java) {
            builder.build(draft, baseTheme, existingThemeIds = emptySet())
        }
    }

    private companion object {
        const val OceanAccent = 0xFF0099CC.toInt()
    }
}

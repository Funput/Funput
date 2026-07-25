package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.roundToInt

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
            overrides = CustomThemeOverrides(
                accentColor = OceanAccent,
                keyBackgroundOpacity = KeyOpacity,
            ),
        )

        val descriptor = builder.build(draft, baseTheme, existingThemeIds = setOf(baseTheme.id))

        assertEquals(KeyboardThemeId.of("custom.my-ocean"), descriptor.id)
        assertEquals("My Ocean", descriptor.name)
        assertEquals("Me", descriptor.author)
        assertEquals(KeyboardThemeOrigin.CUSTOM, descriptor.origin)
        assertEquals(baseTheme.id, descriptor.baseThemeId)
        assertEquals(draft.backgroundImage, descriptor.backgroundImage)
        assertEquals(OceanAccent, descriptor.theme.accentColor)
        // The control scales what the base already draws rather than assigning a flat alpha.
        assertEquals(
            ((baseTheme.theme.keyColor ushr AlphaShift) * KeyOpacity).roundToInt(),
            descriptor.theme.keyColor ushr AlphaShift,
        )
    }

    @Test
    fun buildKeepsATransparentKeySurfaceTransparent() {
        val platelessBase = baseTheme.copy(
            theme = baseTheme.theme.copy(keyColor = 0x00000000, specialKeyColor = 0x00000000),
        )
        val draft = CustomThemeDraft(
            name = "Ocean",
            baseThemeId = platelessBase.id,
            overrides = CustomThemeOverrides(keyBackgroundOpacity = KeyOpacity),
        )

        val descriptor = builder.build(draft, platelessBase, existingThemeIds = emptySet())

        assertEquals(0, descriptor.theme.keyColor ushr AlphaShift)
        assertEquals(0, descriptor.theme.specialKeyColor ushr AlphaShift)
    }

    @Test
    fun buildScalesKeyOpacityAgainstAnOpaqueBase() {
        val paperTheme = LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Light)
        val draft = CustomThemeDraft(
            name = "Ocean",
            baseThemeId = paperTheme.id,
            overrides = CustomThemeOverrides(keyBackgroundOpacity = KeyOpacity),
        )

        val descriptor = builder.build(draft, paperTheme, existingThemeIds = emptySet())

        assertEquals(KeyAlpha, descriptor.theme.keyColor ushr AlphaShift)
        assertEquals(KeyAlpha, descriptor.theme.specialKeyColor ushr AlphaShift)
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

    @Test
    fun buildCanKeepExistingThemeIdForEdits() {
        val editedId = KeyboardThemeId.of("custom.my-ocean")
        val draft = CustomThemeDraft(name = "Ocean 2", baseThemeId = baseTheme.id)

        val descriptor = builder.build(
            draft = draft,
            baseTheme = baseTheme,
            existingThemeIds = emptySet(),
            themeId = editedId,
        )

        assertEquals(editedId, descriptor.id)
        assertEquals("Ocean 2", descriptor.name)
    }

    private companion object {
        const val OceanAccent = 0xFF0099CC.toInt()
        const val KeyOpacity = 0.62f
        const val KeyAlpha = 158
        const val AlphaShift = 24
    }
}

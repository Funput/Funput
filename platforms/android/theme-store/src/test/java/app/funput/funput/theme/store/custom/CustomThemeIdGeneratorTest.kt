package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomThemeIdGeneratorTest {
    private val generator = CustomThemeIdGenerator()

    @Test
    fun generateCreatesPortableIdentifierFromVietnameseName() {
        assertEquals(
            KeyboardThemeId.of("custom.tim-mong-mo"),
            generator.generate("Tím mộng mơ", emptySet()),
        )
    }

    @Test
    fun generateFallsBackWhenNameHasNoPortableCharacters() {
        assertEquals(
            KeyboardThemeId.of("custom.theme"),
            generator.generate("✨✨✨", emptySet()),
        )
    }

    @Test
    fun generateAppendsSuffixWhenIdentifierAlreadyExists() {
        val existingIds = setOf(
            KeyboardThemeId.of("custom.ocean"),
            KeyboardThemeId.of("custom.ocean-2"),
        )

        assertEquals(
            KeyboardThemeId.of("custom.ocean-3"),
            generator.generate("Ocean", existingIds),
        )
    }
}

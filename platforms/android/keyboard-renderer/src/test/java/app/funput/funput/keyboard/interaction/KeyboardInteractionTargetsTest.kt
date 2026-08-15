package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.layout.KeyboardGeometry
import app.funput.funput.keyboard.layout.KeyboardGeometrySpec
import app.funput.funput.keyboard.layout.KeyboardLayouts
import app.funput.funput.keyboard.layout.qwertyLayout
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.SuggestionSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardInteractionTargetsTest {
    private val spec = KeyboardGeometrySpec(
        horizontalPadding = 21f,
        verticalPadding = 24f,
        horizontalGap = 0f,
        verticalGap = 0f,
        horizontalGapRatio = 0.11f,
        verticalGapRatio = 0.16f,
        keyAspectRatio = 0.75f,
        suggestionBarHeight = 126f,
        suggestionBarGap = 18f,
    )
    private val keyboard = KeyboardGeometry.resolve(
        layout = qwertyLayout(
            id = "test-suggestions",
            inputMethod = KeyboardInputMethod.TELEX,
            actionKeys = KeyboardLayouts.telex.rows.last().keys,
            showSuggestionBar = true,
        ).copy(
            suggestionBar = requireNotNull(KeyboardLayouts.telex.suggestionBar).copy(
                suggestionsEnabled = true,
            ),
        ),
        width = 1080f,
        height = 726f,
        spec = spec,
    )

    @Test
    fun eachVisibleSuggestionHasItsOwnTarget() {
        val bounds = requireNotNull(keyboard.suggestionBar).suggestionsBounds
        val segmentWidth = bounds.width / SuggestionCount

        repeat(SuggestionCount) { index ->
            val x = bounds.left + segmentWidth * (index + 0.5f)
            assertEquals(
                SuggestionTargetIds.id(index),
                keyboard.interactionTargetAt(x, bounds.centerY, SuggestionCount),
            )
        }
    }

    @Test
    fun emptySuggestionListLeavesBarNonInteractive() {
        val bounds = requireNotNull(keyboard.suggestionBar).suggestionsBounds

        assertNull(keyboard.interactionTargetAt(bounds.centerX, bounds.centerY, 0))
    }

    @Test
    fun clipboardUsesWholeContentRegionOnlyWhenSuggestionsAreEmpty() {
        val bounds = requireNotNull(keyboard.suggestionBar).suggestionsBounds

        assertEquals(
            ClipboardTargetId,
            keyboard.interactionTargetAt(bounds.right - 1f, bounds.centerY, 0, clipboardVisible = true),
        )
        assertEquals(
            SuggestionTargetIds.id(1),
            keyboard.interactionTargetAt(bounds.centerX, bounds.centerY, 3, clipboardVisible = true),
        )
    }

    @Test
    fun targetResolvesBackToSelectionData() {
        val suggestions = listOf("mình", "chào", "bạn")

        assertEquals(
            SuggestionSelection(index = 1, text = "chào"),
            suggestions.selectionForTarget(SuggestionTargetIds.id(1)),
        )
    }

    @Test
    fun emojiStillResolvesAsAKeyTarget() {
        val emoji = requireNotNull(keyboard.suggestionBar).emojiKey

        assertEquals(
            emoji.spec.id,
            keyboard.interactionTargetAt(emoji.bounds.centerX, emoji.bounds.centerY, SuggestionCount),
        )
    }

    private companion object {
        const val SuggestionCount = 3
    }
}

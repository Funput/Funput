package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.SuggestionSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorCompletionSessionTest {
    private val committed = mutableListOf<FakeCompletion>()
    private val suggestionChanges = mutableListOf<List<String>>()
    private val session = EditorCompletionSession<FakeCompletion>(
        text = FakeCompletion::text,
        commit = { completion -> committed.add(completion) },
        onSuggestionsChanged = suggestionChanges::add,
    )

    @Test
    fun `enabled session filters empty values and exposes at most three`() {
        session.configure(enabled = true)
        session.update(
            arrayOf(
                FakeCompletion("one"),
                FakeCompletion(""),
                FakeCompletion("two"),
                FakeCompletion("three"),
                FakeCompletion("four"),
            ),
        )

        assertEquals(listOf("one", "two", "three"), suggestionChanges.last())
        assertTrue(session.select(SuggestionSelection(index = 1, text = "two")))
        assertEquals(FakeCompletion("two"), committed.single())
    }

    @Test
    fun `disabled session clears values and rejects selection`() {
        session.configure(enabled = true)
        session.update(arrayOf(FakeCompletion("one")))

        session.configure(enabled = false)

        assertEquals(emptyList<String>(), suggestionChanges.last())
        assertFalse(session.select(SuggestionSelection(index = 0, text = "one")))
    }
}

private data class FakeCompletion(val text: String)

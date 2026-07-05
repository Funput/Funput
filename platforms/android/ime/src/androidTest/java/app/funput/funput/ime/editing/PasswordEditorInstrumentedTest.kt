package app.funput.funput.ime.editing

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.funput.funput.ime.editing.support.ImeEditingScenario
import app.funput.funput.ime.editing.support.onMainThread
import app.funput.funput.ime.editing.support.type
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Password fields must bypass Vietnamese composition and commit raw keystrokes. */
@RunWith(AndroidJUnit4::class)
class PasswordEditorInstrumentedTest {
    @Test
    fun passwordEditorCommitsRawAsciiWithoutComposition() = onMainThread {
        val policy = EditorInfoPolicyResolver.resolve(passwordEditorInfo())
        val allowComposition = policy.editorMode.supportsVietnameseComposition

        ImeEditingScenario.create(allowComposition = allowComposition).use { scenario ->
            scenario.handler.type("vieejt")

            assertEquals("vieejt", scenario.text)
        }
    }

    @Test
    fun textEditorStillComposes() = onMainThread {
        ImeEditingScenario.create().use { scenario ->
            scenario.handler.type("vieejt")

            assertEquals("việt", scenario.text)
        }
    }

    private fun passwordEditorInfo() = EditorInfo().apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
}

package app.funput.funput.ime.editing.entersend

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.editing.EditorInfoPolicyResolver
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeEditorAction
import app.funput.funput.keyboard.model.KeyboardEnterAction
import org.junit.Assert.assertEquals
import org.junit.Test

/** Messenger's message box, as `dumpsys input_method` reported it on a Galaxy S21 Ultra. */
private const val MessengerInputType = 0x24001
private const val MessengerImeOptions = 0x40000006
private const val Messenger = "com.facebook.orca"

class EnterToSendPolicyTest {
    @Test fun `messenger's message box sends when messenger is chosen`() {
        val action = resolve(MessengerInputType, MessengerImeOptions).editorAction

        assertEquals(KeyboardEnterAction.Standard.SEND, action.presentation)
        // Send, not the DONE Messenger declares: DONE only hides the keyboard there.
        assertEquals(ImeEditCommand.PerformEditorAction(EditorInfo.IME_ACTION_SEND), action.command)
    }

    @Test fun `an app that is not chosen keeps its new line`() {
        val action = resolve(MessengerInputType, MessengerImeOptions, packageName = "com.example.notes").editorAction

        assertEquals(ImeEditorAction.NewLine, action)
    }

    @Test fun `nothing changes when no app is chosen`() {
        assertEquals(ImeEditorAction.NewLine, resolve(MessengerInputType, MessengerImeOptions, packages = emptySet()).editorAction)
    }

    @Test fun `single line fields and fields with their own action are left alone`() {
        val singleLine = resolve(InputType.TYPE_CLASS_TEXT, MessengerImeOptions)
        val search = resolve(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_SEARCH)

        assertEquals(ImeEditorAction.NewLine, singleLine.editorAction)
        assertEquals(KeyboardEnterAction.Standard.SEARCH, search.editorAction.presentation)
    }

    @Test fun `password and number fields never send`() {
        val password = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE

        assertEquals(ImeEditorAction.NewLine, resolve(password, MessengerImeOptions).editorAction)
        assertEquals(ImeEditorAction.NewLine, resolve(InputType.TYPE_CLASS_NUMBER, MessengerImeOptions).editorAction)
    }

    @Test fun `a missing package name never matches`() {
        assertEquals(ImeEditorAction.NewLine, resolve(MessengerInputType, MessengerImeOptions, packageName = null).editorAction)
    }

    private fun resolve(
        inputType: Int,
        imeOptions: Int,
        packageName: String? = Messenger,
        packages: Set<String> = setOf(Messenger),
    ): EditorInfoPolicy = EnterToSendPolicy.apply(
        policy = EditorInfoPolicyResolver.resolve(inputType, imeOptions, packageName = packageName),
        packageName = packageName,
        packages = packages,
    )
}

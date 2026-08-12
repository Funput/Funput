package app.funput.funput.ime.clipboard.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.clipboard.controller.ImeClipboardController
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardGateway
import app.funput.funput.ime.clipboard.platform.ClipboardObservation
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult
import app.funput.funput.ime.clipboard.platform.ClipboardSnapshot
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.ui.FunputKeyboardView
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImeClipboardUiBindingInstrumentedTest {
    @Test
    fun replacingViewClearsOldCallbackAndPastesOnlyFromCurrentView() = onMain {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = Files.createTempDirectory("clipboard-ui-binding").toFile()
        val committed = mutableListOf<String>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = ImeClipboardController(
            parentScope = scope,
            preferences = MutableStateFlow(ClipboardPreferences.Default),
            gateway = BindingGateway,
            storeFactory = { ClipboardHistoryStore(directory, it) },
            commitText = committed::add,
            afterCommit = {},
            ioDispatcher = Dispatchers.Unconfined,
        )
        val binding = ImeClipboardUiBinding(context, scope, controller)
        val first = FunputKeyboardView(context)
        val second = FunputKeyboardView(context)

        binding.attach(first)
        controller.start(KeyboardEditorMode.TEXT)
        assertSame(KeyboardClipboardHint.TEXT, first.clipboardHint)
        binding.attach(second)
        assertNull(first.clipboardHint)
        assertNull(first.callbacks.onClipboardPasteRequested)
        assertSame(KeyboardClipboardHint.TEXT, second.clipboardHint)
        second.callbacks.onClipboardPasteRequested?.invoke()
        assertEquals(listOf("clipboard"), committed)
        assertNull(second.clipboardHint)

        binding.close()
        controller.close()
    }

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
}

private object BindingGateway : ClipboardGateway {
    private const val Token = "android:v1:timestamp:10"
    override fun snapshot() = ClipboardSnapshot(ClipboardContentKind.TEXT, false, Token)
    override fun observe(onChanged: () -> Unit) = ClipboardObservation {}
    override fun readText(maxLength: Int) = ClipboardReadResult.Success(
        "clipboard", ClipboardContentKind.TEXT, false, Token,
    )
}

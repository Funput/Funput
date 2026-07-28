package app.funput.funput.ime.editing.support

import android.content.Context
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod

/** A real [EditText] plus its live [InputConnection], mirroring a host editor buffer. */
internal class HostEditor(context: Context) {
    val editText = EditText(context)
    val connection: InputConnection =
        editText.onCreateInputConnection(EditorInfo()) ?: FallbackConnection(editText)

    val text: String get() = editText.text.toString()

    fun moveCursorTo(index: Int) {
        connection.setSelection(index, index)
    }

    /** Guards against devices that refuse a connection for an unattached view. */
    private class FallbackConnection(view: EditText) : BaseInputConnection(view, true) {
        private val target = view
        override fun getEditable() = target.editableText
    }
}

/** Wires the real editing pipeline and JNI engine onto a [HostEditor] for one test. */
internal class ImeEditingScenario private constructor(
    val host: HostEditor,
    private val engine: NativeVietnameseEngine,
    val composition: AndroidCompositionSession,
    val handler: ImeKeyActionHandler,
) : AutoCloseable {
    val text: String get() = host.text

    override fun close() {
        handler.finish()
        engine.close()
    }

    companion object {
        fun create(
            inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX,
            allowComposition: Boolean = true,
        ): ImeEditingScenario {
            val host = HostEditor(ApplicationProvider.getApplicationContext())
            val engine = NativeVietnameseEngine()
            val composition = AndroidCompositionSession(engine)
            val handler = ImeKeyActionHandler(
                composition = composition,
                editor = InputConnectionEditor(),
                connection = { host.connection },
                enterCommand = { ImeEditCommand.CommitText("\n") },
            )
            engine.configure(
                EngineConfiguration(
                    inputMethod = inputMethod,
                    toneStyle = ToneStyle.TRADITIONAL,
                    smartRestore = true,
                    eagerRestore = true,
                    spellCheck = true,
                ),
            )
            handler.start(allowComposition = allowComposition)
            return ImeEditingScenario(host, engine, composition, handler)
        }
    }
}

/**
 * Runs [block] on the main thread, where editor buffers and input connections live.
 * Any assertion failure is rethrown on the test thread by [runOnMainSync].
 */
internal fun onMainThread(block: () -> Unit) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
}

/** Feeds [text] to the handler one key at a time, as the touch layer would. */
internal fun ImeKeyActionHandler.type(text: String) {
    text.forEach { char ->
        onKeyAction(
            if (char == ' ') KeyAction.Space else KeyAction.Input("key-$char", char.toString()),
        )
    }
}

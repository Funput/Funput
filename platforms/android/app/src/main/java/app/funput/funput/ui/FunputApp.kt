package app.funput.funput.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.playground.PlaygroundTextBuffer
import app.funput.funput.ui.playground.PlaygroundTextBufferSaver
import app.funput.funput.ui.playground.PlaygroundCallbackReducer
import app.funput.funput.ui.playground.PlaygroundKeyActionReducer
import app.funput.funput.ui.theme.FunputTheme
import kotlinx.coroutines.launch

@Composable
fun FunputApp() {
    val context = LocalContext.current
    val inputMethodSettings = remember(context) { InputMethodSettings(context) }
    val inputMethod by inputMethodSettings.inputMethod.collectAsState(
        initial = InputMethodSettings.DefaultInputMethod,
    )
    val coroutineScope = rememberCoroutineScope()
    var lastAction by remember { mutableStateOf<KeyAction?>(null) }
    var actionCount by remember { mutableIntStateOf(0) }
    var lastUiCallback by remember { mutableStateOf("—") }
    var textBuffer by rememberSaveable(stateSaver = PlaygroundTextBufferSaver) {
        mutableStateOf(PlaygroundTextBuffer.from("Funput playground 👋"))
    }

    FunputTheme(dynamicColor = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            KeyboardPreviewScreen(
                inputMethod = inputMethod,
                onInputMethodSelected = { method ->
                    coroutineScope.launch { inputMethodSettings.setInputMethod(method) }
                },
                lastAction = lastAction,
                actionCount = actionCount,
                lastUiCallback = lastUiCallback,
                textBuffer = textBuffer,
                onTextCursorChanged = { offset -> textBuffer = textBuffer.moveCursorTo(offset) },
                onTextCleared = { textBuffer = textBuffer.clear() },
                onKeyAction = { action ->
                    textBuffer = PlaygroundKeyActionReducer.reduce(textBuffer, action)
                    actionCount = if (action == lastAction) actionCount + 1 else 1
                    lastAction = action
                },
                onEmojiPanelOpened = { lastUiCallback = "Emoji panel opened" },
                onEmojiSelected = { emoji ->
                    textBuffer = PlaygroundCallbackReducer.emojiSelected(textBuffer, emoji)
                    lastUiCallback = "Emoji: $emoji"
                },
                onSuggestionSelected = { selection ->
                    textBuffer = PlaygroundCallbackReducer.suggestionSelected(textBuffer, selection)
                    lastUiCallback = "Suggestion: ${selection.text}"
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FunputAppPreview() {
    FunputApp()
}

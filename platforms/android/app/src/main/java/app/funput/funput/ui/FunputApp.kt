package app.funput.funput.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.theme.FunputTheme

@Composable
fun FunputApp() {
    var inputMethod by rememberSaveable { mutableStateOf(KeyboardInputMethod.TELEX) }
    var lastAction by remember { mutableStateOf<KeyAction?>(null) }
    var actionCount by remember { mutableIntStateOf(0) }
    var lastUiCallback by remember { mutableStateOf("—") }

    FunputTheme(dynamicColor = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            KeyboardPreviewScreen(
                inputMethod = inputMethod,
                onInputMethodSelected = { inputMethod = it },
                lastAction = lastAction,
                actionCount = actionCount,
                lastUiCallback = lastUiCallback,
                onKeyAction = { action ->
                    actionCount = if (action == lastAction) actionCount + 1 else 1
                    lastAction = action
                },
                onEmojiPanelOpened = { lastUiCallback = "Emoji panel opened" },
                onEmojiSelected = { emoji -> lastUiCallback = "Emoji: $emoji" },
                onSuggestionSelected = { selection ->
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

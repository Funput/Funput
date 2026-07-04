package app.funput.funput.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.ui.FunputKeyboardView

private val PreviewSuggestions = listOf("mình", "chào", "bạn")

@Composable
internal fun KeyboardPreview(
    inputMethod: KeyboardInputMethod,
    onKeyAction: (KeyAction) -> Unit,
    onEmojiPanelOpened: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onSuggestionSelected: (SuggestionSelection) -> Unit,
) {
    AndroidView(
        factory = { context -> FunputKeyboardView(context) },
        update = { keyboard ->
            keyboard.inputMethod = inputMethod
            keyboard.suggestions = PreviewSuggestions
            keyboard.callbacks.onKeyAction = onKeyAction
            keyboard.callbacks.onEmojiPanelOpened = onEmojiPanelOpened
            keyboard.callbacks.onEmojiSelected = onEmojiSelected
            keyboard.callbacks.onSuggestionSelected = onSuggestionSelected
        },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(FunputKeyboardView.recommendedHeightDp(inputMethod).dp)
            .clip(RoundedCornerShape(18.dp)),
    )
}

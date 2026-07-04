package app.funput.funput.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.SuggestionSelection

private val PreviewSuggestions = listOf("mình", "chào", "bạn")

@Composable
internal fun KeyboardPreviewScreen(
    inputMethod: KeyboardInputMethod,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    lastAction: KeyAction?,
    actionCount: Int,
    lastUiCallback: String,
    onKeyAction: (KeyAction) -> Unit,
    onEmojiRequested: () -> Unit,
    onSuggestionSelected: (SuggestionSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
    ) {
        PreviewHeader(
            inputMethod = inputMethod,
            onInputMethodSelected = onInputMethodSelected,
            lastAction = lastAction,
            actionCount = actionCount,
            lastUiCallback = lastUiCallback,
        )
        Spacer(modifier = Modifier.height(24.dp))
        KeyboardPreview(
            inputMethod = inputMethod,
            onKeyAction = onKeyAction,
            onEmojiRequested = onEmojiRequested,
            onSuggestionSelected = onSuggestionSelected,
        )
    }
}

@Composable
private fun PreviewHeader(
    inputMethod: KeyboardInputMethod,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    lastAction: KeyAction?,
    actionCount: Int,
    lastUiCallback: String,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(stringResource(R.string.keyboard_preview_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.keyboard_preview_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.input_method_label), style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputMethodChip(
                stringResource(R.string.input_method_telex),
                inputMethod == KeyboardInputMethod.TELEX,
            ) { onInputMethodSelected(KeyboardInputMethod.TELEX) }
            InputMethodChip(
                stringResource(R.string.input_method_vni),
                inputMethod == KeyboardInputMethod.VNI,
            ) { onInputMethodSelected(KeyboardInputMethod.VNI) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.last_key_action, lastAction.previewLabel(actionCount)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            stringResource(R.string.last_ui_callback, lastUiCallback),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun KeyboardPreview(
    inputMethod: KeyboardInputMethod,
    onKeyAction: (KeyAction) -> Unit,
    onEmojiRequested: () -> Unit,
    onSuggestionSelected: (SuggestionSelection) -> Unit,
) {
    AndroidView(
        factory = { context -> KeyboardSurfaceView(context) },
        update = { keyboard ->
            keyboard.inputMethod = inputMethod
            keyboard.suggestions = PreviewSuggestions
            keyboard.callbacks.onKeyAction = onKeyAction
            keyboard.callbacks.onEmojiRequested = onEmojiRequested
            keyboard.callbacks.onSuggestionSelected = onSuggestionSelected
        },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(KeyboardSurfaceView.recommendedHeightDp(inputMethod).dp)
            .clip(RoundedCornerShape(18.dp)),
    )
}

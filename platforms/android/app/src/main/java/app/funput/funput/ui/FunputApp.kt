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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.theme.FunputTheme

private val PreviewSuggestions = listOf("mình", "chào", "bạn")

@Composable
fun FunputApp() {
    var inputMethod by rememberSaveable { mutableStateOf(KeyboardInputMethod.TELEX) }
    var lastAction by remember { mutableStateOf<KeyAction?>(null) }

    FunputTheme(dynamicColor = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            KeyboardPreviewScreen(
                inputMethod = inputMethod,
                onInputMethodSelected = { selectedMethod -> inputMethod = selectedMethod },
                lastAction = lastAction,
                onKeyAction = { action -> lastAction = action },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun KeyboardPreviewScreen(
    inputMethod: KeyboardInputMethod,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    lastAction: KeyAction?,
    onKeyAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.keyboard_preview_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.keyboard_preview_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.input_method_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputMethodChip(
                    label = stringResource(R.string.input_method_telex),
                    selected = inputMethod == KeyboardInputMethod.TELEX,
                    onClick = { onInputMethodSelected(KeyboardInputMethod.TELEX) },
                )
                InputMethodChip(
                    label = stringResource(R.string.input_method_vni),
                    selected = inputMethod == KeyboardInputMethod.VNI,
                    onClick = { onInputMethodSelected(KeyboardInputMethod.VNI) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.last_key_action, lastAction.previewLabel()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        AndroidView(
            factory = { context ->
                KeyboardSurfaceView(context).apply {
                    this.inputMethod = inputMethod
                    suggestions = PreviewSuggestions
                    this.onKeyAction = onKeyAction
                }
            },
            update = { keyboardView ->
                keyboardView.inputMethod = inputMethod
                keyboardView.suggestions = PreviewSuggestions
                keyboardView.onKeyAction = onKeyAction
            },
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(KeyboardSurfaceView.recommendedHeightDp(inputMethod).dp)
                .clip(RoundedCornerShape(18.dp)),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FunputAppPreview() {
    FunputApp()
}

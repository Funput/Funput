package app.funput.funput.keyboard.ui.speech

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

@Composable
internal fun SpeechPanelControls(
    state: SpeechPanelState,
    palette: KeyboardPanelPalette,
    onAction: (SpeechPanelAction) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (state.stage == SpeechPanelStage.LISTENING) {
            Action(R.string.speech_panel_stop, palette) { onAction(SpeechPanelAction.STOP) }
        }
        if (state.stage == SpeechPanelStage.ERROR && state.canOpenSetup) {
            Action(R.string.speech_panel_setup, palette) { onAction(SpeechPanelAction.OPEN_SETUP) }
        } else if (state.stage == SpeechPanelStage.ERROR && state.canRetry) {
            Action(R.string.speech_panel_retry, palette) { onAction(SpeechPanelAction.RETRY) }
        }
        Action(if (state.stage == SpeechPanelStage.ERROR) R.string.speech_panel_return
            else R.string.speech_panel_cancel, palette) { onAction(SpeechPanelAction.CANCEL) }
    }
}

@Composable
private fun Action(label: Int, palette: KeyboardPanelPalette, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(Color(palette.buttonSurface))
            .clickable(role = Role.Button, onClick = onClick).padding(12.dp, 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(stringResource(label), style = TextStyle(
            color = Color(palette.readableOn(palette.buttonSurface, palette.label)),
            fontSize = 14.sp, textAlign = TextAlign.Center,
        ))
    }
}

package app.funput.funput.keyboard.ui.speech

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

@Composable
internal fun SpeechPanelContent(
    state: SpeechPanelState,
    palette: KeyboardPanelPalette,
    onAction: (SpeechPanelAction) -> Unit,
) {
    val title = when (state.stage) {
        SpeechPanelStage.PREPARING -> R.string.speech_panel_preparing
        SpeechPanelStage.LISTENING -> R.string.speech_panel_listening
        SpeechPanelStage.FINALIZING -> R.string.speech_panel_finalizing
        SpeechPanelStage.ERROR -> R.string.speech_panel_error
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp, 12.dp)) {
            // Announce status changes only; partial hypotheses are not a live region.
            BasicText(stringResource(title), Modifier.fillMaxWidth().semantics {
                liveRegion = LiveRegionMode.Polite
            }, TextStyle(Color(palette.readable(palette.label)), 18.sp, fontWeight = FontWeight.Medium))
            BasicText(stringResource(if (state.language == KeyboardLanguage.VIETNAMESE)
                R.string.speech_panel_vi else R.string.speech_panel_en), Modifier.padding(top = 4.dp),
                TextStyle(Color(palette.readable(palette.secondaryLabel)), 13.sp))
            state.message?.let { BasicText(it, Modifier.padding(top = 12.dp),
                TextStyle(Color(palette.readable(palette.label)), 14.sp)) }
            if (state.stage != SpeechPanelStage.ERROR) {
                BasicText(state.preview.ifEmpty { stringResource(R.string.speech_panel_preview_hint) },
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    TextStyle(Color(palette.readable(palette.label)), 17.sp))
            }
        }
        SpeechPanelControls(state, palette, onAction)
    }
}

package app.funput.funput.keyboard.ui.speech

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.keyboard.ui.speech.visual.SpeechPanelHeader
import app.funput.funput.keyboard.ui.speech.visual.SpeechPanelIndicator
import app.funput.funput.keyboard.ui.speech.visual.SpeechPanelStyle

@Composable
internal fun SpeechPanelContent(
    state: SpeechPanelState,
    palette: KeyboardPanelPalette,
    style: SpeechPanelStyle,
    onAction: (SpeechPanelAction) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    Column(Modifier.fillMaxSize().padding(16.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!largeText) SpeechPanelHeader(state, palette, style)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                if (largeText) SpeechPanelHeader(state, palette, style)
                if (state.stage == SpeechPanelStage.ERROR || state.preview.isEmpty()) {
                    SpeechPanelIndicator(state.stage, style)
                    val hint = state.message ?: stringResource(when (state.stage) {
                        SpeechPanelStage.PREPARING -> R.string.speech_panel_preparing_hint
                        SpeechPanelStage.LISTENING -> R.string.speech_panel_listening_hint
                        SpeechPanelStage.FINALIZING -> R.string.speech_panel_finalizing_hint
                        SpeechPanelStage.ERROR -> R.string.speech_panel_error_hint
                    })
                    BasicText(hint, Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        TextStyle(Color(palette.readable(palette.secondaryLabel)), 14.sp,
                            lineHeight = 20.sp, textAlign = TextAlign.Center))
                } else {
                    Column(Modifier.fillMaxWidth().background(Color(style.cardSurface),
                        RoundedCornerShape(style.radiusDp.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicText(stringResource(R.string.speech_panel_transcript),
                            style = TextStyle(Color(style.cardSecondary), 11.sp, fontWeight = FontWeight.Medium))
                        // Hypotheses are readable but never a live region; only the header announces state.
                        BasicText(state.preview, Modifier.fillMaxWidth(),
                            TextStyle(Color(style.cardLabel), 18.sp, lineHeight = 26.sp))
                    }
                }
            }
        }
        SpeechPanelControls(state, style, onAction)
    }
}

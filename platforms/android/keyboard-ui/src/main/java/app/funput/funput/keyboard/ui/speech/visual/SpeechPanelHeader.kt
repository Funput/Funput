package app.funput.funput.keyboard.ui.speech.visual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage
import app.funput.funput.keyboard.ui.speech.SpeechPanelState

@Composable
internal fun SpeechPanelHeader(state: SpeechPanelState, palette: KeyboardPanelPalette, style: SpeechPanelStyle) {
    val title = stringResource(when (state.stage) {
        SpeechPanelStage.PREPARING -> R.string.speech_panel_preparing
        SpeechPanelStage.LISTENING -> R.string.speech_panel_listening
        SpeechPanelStage.FINALIZING -> R.string.speech_panel_finalizing
        SpeechPanelStage.ERROR -> R.string.speech_panel_error
    })
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Status(title, palette)
                Language(state.language, style)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Status(title, palette)
                Language(state.language, style)
            }
        }
    }
}

@Composable
private fun Status(title: String, palette: KeyboardPanelPalette) {
    BasicText(title, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        TextStyle(Color(palette.readable(palette.label)), 16.sp, fontWeight = FontWeight.SemiBold))
}

@Composable
private fun Language(language: KeyboardLanguage, style: SpeechPanelStyle) {
    BasicText(stringResource(if (language == KeyboardLanguage.VIETNAMESE) R.string.speech_panel_vi
        else R.string.speech_panel_en),
        Modifier.background(Color(style.secondarySurface), RoundedCornerShape(50)).padding(10.dp, 5.dp),
        TextStyle(Color(style.secondaryLabel), 11.sp, fontWeight = FontWeight.Medium))
}

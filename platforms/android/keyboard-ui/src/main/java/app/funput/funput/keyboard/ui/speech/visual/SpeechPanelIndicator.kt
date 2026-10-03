package app.funput.funput.keyboard.ui.speech.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage

/** Decorative session activity, not a waveform, audio level or recognition progress. */
@Composable
internal fun SpeechPanelIndicator(
    stage: SpeechPanelStage,
    style: SpeechPanelStyle,
    motionVisible: Boolean,
    diameter: Dp,
) {
    val phase = rememberSpeechOrbPhase(stage, motionVisible)
    Canvas(Modifier.size(diameter).testTag("speech-orb")) {
        drawSpeechOrb(style, stage, phase.value)
    }
}

package app.funput.funput.keyboard.ui.speech.visual

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.MotionDurationScale
import app.funput.funput.keyboard.ui.speech.SpeechPanelStage

internal fun speechOrbShouldAnimate(stage: SpeechPanelStage, visible: Boolean, scale: Float): Boolean =
    visible && scale > 0f && stage != SpeechPanelStage.ERROR

@Composable
internal fun rememberSpeechOrbPhase(stage: SpeechPanelStage, visible: Boolean): State<Float> {
    val scale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    if (!speechOrbShouldAnimate(stage, visible, scale)) return rememberUpdatedState(0f)
    val transition = rememberInfiniteTransition(label = "Speech session activity")
    return transition.animateFloat(0f, 1f, infiniteRepeatable(
        animation = tween(6400, easing = LinearEasing), repeatMode = RepeatMode.Restart,
    ), label = "Orb flow")
}

package app.funput.funput.keyboard.ui.speech

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.speech.visual.SpeechPanelStyle

@Composable
internal fun SpeechPanelControls(state: SpeechPanelState, style: SpeechPanelStyle, onAction: (SpeechPanelAction) -> Unit) {
    val primary = when {
        state.stage == SpeechPanelStage.LISTENING -> R.string.speech_panel_stop to SpeechPanelAction.STOP
        state.stage == SpeechPanelStage.ERROR && state.canOpenSetup -> R.string.speech_panel_setup to SpeechPanelAction.OPEN_SETUP
        state.stage == SpeechPanelStage.ERROR && state.canRetry -> R.string.speech_panel_retry to SpeechPanelAction.RETRY
        else -> null
    }
    val cancel = if (state.stage == SpeechPanelStage.ERROR) R.string.speech_panel_back else R.string.speech_panel_cancel
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = state.stage == SpeechPanelStage.ERROR &&
            (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f)
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                primary?.let { Action(it.first, true, style, Modifier.fillMaxWidth()) { onAction(it.second) } }
                Action(cancel, false, style, Modifier.fillMaxWidth()) { onAction(SpeechPanelAction.CANCEL) }
            }
        } else {
            Row(Modifier.widthIn(max = 320.dp).align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Action(cancel, false, style, Modifier.weight(1f)) { onAction(SpeechPanelAction.CANCEL) }
                primary?.let { Action(it.first, true, style, Modifier.weight(1f)) { onAction(it.second) } }
            }
        }
    }
}

@Composable
private fun Action(label: Int, primary: Boolean, style: SpeechPanelStyle, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(percent = 50))
        .background(Color(if (primary) style.primarySurface else style.secondarySurface))
        .clickable(role = Role.Button, onClick = onClick).padding(12.dp, 12.dp), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val ink = Color(if (primary) style.primaryLabel else style.secondaryLabel)
            if (LocalDensity.current.fontScale <= 1.3f &&
                (label == R.string.speech_panel_stop || label == R.string.speech_panel_cancel)) {
                Canvas(Modifier.size(14.dp)) {
                    if (primary) drawRoundRect(ink, Offset(size.width * .12f, size.height * .12f),
                        Size(size.width * .76f, size.height * .76f), CornerRadius(size.width * .16f))
                    else {
                        drawLine(ink, Offset.Zero, Offset(size.width, size.height), 2.dp.toPx(), StrokeCap.Round)
                        drawLine(ink, Offset(size.width, 0f), Offset(0f, size.height), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            BasicText(stringResource(label), style = TextStyle(ink, 14.sp,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium, textAlign = TextAlign.Center))
        }
    }
}

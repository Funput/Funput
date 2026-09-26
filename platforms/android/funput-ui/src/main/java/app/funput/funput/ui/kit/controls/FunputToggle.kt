package app.funput.funput.ui.kit.controls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputMotion
import app.funput.funput.ui.kit.theme.FunputUi

private val TrackWidth = 51.dp
private val TrackHeight = 31.dp
private val ThumbInset = 2.dp
private val ThumbSize = TrackHeight - ThumbInset * 2

/**
 * An on/off switch: an accent track when on, a neutral one when off, a white thumb that slides.
 *
 * With [onCheckedChange] `null` it is purely visual; a `ToggleRow` uses it that way so the whole
 * row is the switch. Given a callback it becomes a switch on its own, with a 48dp touch target.
 */
@Composable
fun FunputToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FunputUi.colors
    val track by animateColorAsState(
        if (checked) colors.accent else colors.tertiaryLabel,
        FunputMotion.selection(),
        label = "toggle-track",
    )
    val thumbX by animateDpAsState(
        if (checked) TrackWidth - ThumbSize - ThumbInset * 2 else 0.dp,
        FunputMotion.selection(),
        label = "toggle-thumb",
    )
    val interactive = if (onCheckedChange != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        Modifier
    }
    Box(modifier.then(interactive), contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .size(TrackWidth, TrackHeight)
                .clip(FunputUi.shapes.capsule)
                .background(track)
                .padding(ThumbInset),
        ) {
            Box(
                Modifier
                    .offset(x = thumbX)
                    .size(ThumbSize)
                    .shadow(2.dp, FunputUi.shapes.capsule)
                    .background(Color.White, FunputUi.shapes.capsule),
            )
        }
    }
}

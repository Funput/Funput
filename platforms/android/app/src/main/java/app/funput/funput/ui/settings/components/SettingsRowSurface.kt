package app.funput.funput.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * The container every settings row is drawn on: its own surface, its own animated corners.
 *
 * [modifier] is where the caller's `clickable` or `toggleable` goes. It is applied after the clip
 * so the ripple stays inside the row's rounded shape, and the same [interactionSource] drives both
 * that ripple and the corner animation, which is why the press and the swell stay in step.
 */
@Composable
internal fun SettingsRowSurface(
    position: RowPosition,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(rememberRowShape(position, interactionSource))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        content = content,
    )
}

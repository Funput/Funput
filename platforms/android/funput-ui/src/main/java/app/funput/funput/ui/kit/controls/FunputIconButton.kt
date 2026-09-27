package app.funput.funput.ui.kit.controls

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * An icon-only action, such as "add" or "options" in a top bar: a 24dp glyph in the accent inside
 * a 48dp touch target. [contentDescription] is required, since the icon is all the user sees.
 */
@Composable
fun FunputIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FunputUi.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(FunputUi.shapes.capsule)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (enabled) colors.accent else colors.tertiaryLabel),
            modifier = Modifier.size(24.dp),
        )
    }
}

package app.funput.funput.ui.settings.enterkey.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUi

/** The size of an app icon in a row: the same as the tinted tile other rows show. */
internal val AppIconSize: Dp = 32.dp

/**
 * An app's own launcher icon, clipped like the row tiles so a list of apps lines up with the
 * rest of Settings. Without one (an uninstalled app), a neutral tile with a generic glyph.
 */
@Composable
internal fun AppIcon(icon: ImageBitmap?) {
    val shape = FunputUi.shapes.iconTile
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(AppIconSize).clip(shape))
        return
    }
    val colors = FunputUi.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(AppIconSize).clip(shape).background(colors.separator),
    ) {
        Image(
            painter = painterResource(FunputIcons.Send),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.secondaryLabel),
            modifier = Modifier.size(20.dp),
        )
    }
}

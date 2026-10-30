package app.funput.funput.ui.settings.enterkey.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputTint

/**
 * A row for one app: its own icon once decoded, and until then (or once it is uninstalled) the
 * page's tile, like any other settings row.
 */
@Composable
internal fun AppRow(
    packageName: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    val icon = rememberAppIcon(packageName)
    FunputRow(
        title = title,
        modifier = modifier,
        summary = summary,
        icon = FunputIcons.Send,
        tint = FunputTint.BLUE,
        accessory = accessory,
        leading = icon?.let { { AppImage(it) } },
    )
}

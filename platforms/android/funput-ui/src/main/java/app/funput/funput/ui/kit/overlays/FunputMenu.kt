package app.funput.funput.ui.kit.overlays

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUi

/** One choice in a [FunputMenu]. */
@Immutable
data class FunputMenuItem(
    val label: String,
    val onClick: () -> Unit,
    @param:DrawableRes val icon: Int? = null,
    /** The current choice of a picker menu, marked with a check. */
    val selected: Boolean = false,
    /** Deletes or resets something, so it says so in colour. */
    val destructive: Boolean = false,
)

/**
 * A short list of actions anchored to what opened it, such as a sort button or a long-pressed
 * row. Picking an item dismisses the menu before its action runs.
 *
 * Material's dropdown, restyled, for its positioning, focus and back handling. Place it in the
 * same parent as its anchor; it opens over the anchor's bounds.
 */
@Composable
fun FunputMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<FunputMenuItem>,
    modifier: Modifier = Modifier,
) {
    val colors = FunputUi.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        shape = FunputUi.shapes.card,
        modifier = modifier,
    ) {
        items.forEach { item ->
            val color = if (item.destructive) colors.destructive else colors.label
            DropdownMenuItem(
                text = { BasicText(item.label, style = FunputUi.typography.body.copy(color = color)) },
                onClick = {
                    onDismiss()
                    item.onClick()
                },
                leadingIcon = item.icon?.let { icon -> { MenuIcon(icon, ColorFilter.tint(color)) } },
                trailingIcon = if (item.selected) {
                    { MenuIcon(FunputIcons.Check, ColorFilter.tint(colors.accent)) }
                } else {
                    null
                },
                modifier = Modifier.semantics { selected = item.selected },
            )
        }
    }
}

@Composable
private fun MenuIcon(@DrawableRes icon: Int, tint: ColorFilter) {
    Image(painterResource(icon), contentDescription = null, colorFilter = tint, modifier = Modifier.size(20.dp))
}

package app.funput.funput.ui.theme.custom.background

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/**
 * The picture's own colours, offered as accents.
 *
 * A background image on a palette that knows nothing about it is the thing that makes a custom
 * theme look assembled rather than designed. These are offered rather than applied: a photograph
 * has several colours in it and which one should lead is a judgement, not a computation.
 */
@Composable
internal fun ImagePaletteRow(
    colours: List<Int>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (colours.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Text(
            text = stringResource(R.string.custom_theme_palette_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.custom_theme_palette_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            colours.forEach { colour ->
                val description = stringResource(R.string.custom_theme_palette_apply)
                Surface(
                    shape = CircleShape,
                    color = Color(colour),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(46.dp)
                        .semantics { contentDescription = description }
                        .clickable(role = Role.Button) { onSelected(colour) },
                ) {}
            }
        }
    }
}

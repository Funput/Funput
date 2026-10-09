package app.funput.funput.ui.kit.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A labelled text field on a card-coloured rounded box. The [label] sits above and also names the
 * field for TalkBack; [error], when set, turns the border red and explains below. Multi-line when
 * [singleLine] is false, growing from [minLines] to [maxLines].
 */
@Composable
fun FunputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val shape = FunputUi.shapes.iconTile
    // The whole field, label and error included, is the decoration, so [modifier] (a test tag,
    // say) lands on the editable node and a tap anywhere on it focuses the text.
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        textStyle = type.body.copy(color = colors.label),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { field ->
            Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight)) {
                BasicText(label, style = type.label.copy(color = colors.secondaryLabel))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(shape)
                        .background(colors.cardBackground)
                        .border(1.dp, if (error != null) colors.destructive else colors.separator, shape)
                        .padding(FunputUi.spacing.medium),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        BasicText(placeholder, style = type.body.copy(color = colors.tertiaryLabel))
                    }
                    field()
                }
                error?.let { BasicText(it, style = type.caption.copy(color = colors.destructive)) }
            }
        },
    )
}

/**
 * A search box: a capsule with a magnifier, the query, and a clear button while there is text.
 * [placeholder] doubles as the field's accessible name; [clearLabel] names the clear button.
 */
@Composable
fun FunputSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = type.body.copy(color = colors.label),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        decorationBox = { field ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(FunputUi.shapes.capsule)
                    .background(colors.label.copy(alpha = 0.06f))
                    .padding(start = FunputUi.spacing.medium),
            ) {
                Image(
                    painter = painterResource(FunputIcons.Search),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.secondaryLabel),
                    modifier = Modifier.size(20.dp),
                )
                Box(Modifier.weight(1f).padding(horizontal = FunputUi.spacing.small)) {
                    if (query.isEmpty()) BasicText(placeholder, style = type.body.copy(color = colors.tertiaryLabel))
                    field()
                }
                if (query.isNotEmpty()) {
                    FunputIconButton(FunputIcons.ClearField, clearLabel, onClick = { onQueryChange("") })
                }
            }
        },
    )
}

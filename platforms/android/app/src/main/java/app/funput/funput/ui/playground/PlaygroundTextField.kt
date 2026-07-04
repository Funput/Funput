package app.funput.funput.ui.playground

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/** Display-only playground field that never creates an Android input connection. */
@Composable
internal fun PlaygroundTextField(
    buffer: PlaygroundTextBuffer,
    onCursorChanged: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldHeader(canClear = buffer.text.isNotEmpty(), onClear = onClear)
        FieldSurface(buffer = buffer, onCursorChanged = onCursorChanged)
    }
}

@Composable
private fun FieldHeader(canClear: Boolean, onClear: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(R.string.playground_text_field_label),
            style = MaterialTheme.typography.labelLarge,
        )
        TextButton(onClick = onClear, enabled = canClear) {
            Text(stringResource(R.string.playground_text_field_clear))
        }
    }
}

@Composable
private fun FieldSurface(
    buffer: PlaygroundTextBuffer,
    onCursorChanged: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val description = stringResource(R.string.playground_text_field_description)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .semantics { contentDescription = description }
            .padding(16.dp),
    ) {
        TextContent(buffer = buffer, onCursorChanged = onCursorChanged)
    }
}

@Composable
private fun TextContent(
    buffer: PlaygroundTextBuffer,
    onCursorChanged: (Int) -> Unit,
) {
    var layout by remember(buffer.text) { mutableStateOf<TextLayoutResult?>(null) }
    val currentCursorChanged by rememberUpdatedState(onCursorChanged)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .pointerInput(buffer.text) {
                detectTapGestures { position ->
                    layout?.getOffsetForPosition(position)?.let(currentCursorChanged)
                }
            },
    ) {
        if (buffer.text.isEmpty()) {
            Text(
                text = stringResource(R.string.playground_text_field_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = textStyle,
            )
        }
        BasicText(
            text = buffer.text,
            modifier = Modifier.fillMaxWidth(),
            style = textStyle,
            onTextLayout = { layout = it },
        )
        Cursor(layout = layout, cursor = buffer.cursor, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BoxScope.Cursor(layout: TextLayoutResult?, cursor: Int, color: Color) {
    val transition = rememberInfiniteTransition(label = "playground-cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "cursor-alpha",
    )
    Canvas(modifier = Modifier.matchParentSize()) {
        val bounds = layout?.getCursorRect(cursor) ?: return@Canvas
        drawRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(2.dp.toPx(), bounds.height.coerceAtLeast(20.dp.toPx())),
        )
    }
}

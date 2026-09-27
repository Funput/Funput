package app.funput.funput.ui.theme.custom.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import app.funput.funput.ui.kit.theme.FunputUi
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * The image with a marker showing which point stays centred on the keyboard.
 *
 * Dragging sets the focal point directly rather than panning a viewport. The preview is a
 * different shape and size from the keyboard, so "drag the picture" would move the framing by an
 * amount that does not correspond to anything the user can see in the result.
 */
@Composable
internal fun BackgroundFocusPicker(
    source: String,
    focalX: Float,
    focalY: Float,
    onFocusChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap by rememberBackgroundBitmap(source)
    val description = stringResource(R.string.custom_theme_background_focus_hint)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PickerHeight)
            .clip(FunputUi.shapes.thumbnail)
            .semantics { contentDescription = description }
            .pointerInput(source) {
                detectTapGestures { offset -> report(offset, size.width, size.height, onFocusChange) }
            }
            .pointerInput(source) {
                detectDragGestures { change, _ ->
                    report(change.position, size.width, size.height, onFocusChange)
                }
            },
    ) {
        bitmap?.let { decoded ->
            Image(
                bitmap = decoded.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().testTag(BackgroundFocusImageTag),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(focalX * size.width, focalY * size.height)
            // Two rings so the marker stays visible over both bright and dark photos.
            drawCircle(Color.Black.copy(alpha = 0.6f), MarkerRadius.toPx() + 1.dp.toPx(), center, style = Stroke(3f))
            drawCircle(Color.White, MarkerRadius.toPx(), center, style = Stroke(3f))
        }
    }
}

private fun report(
    offset: Offset,
    width: Int,
    height: Int,
    onFocusChange: (Float, Float) -> Unit,
) {
    if (width <= 0 || height <= 0) return
    onFocusChange(
        (offset.x / width).coerceIn(0f, 1f),
        (offset.y / height).coerceIn(0f, 1f),
    )
}

private val PickerHeight = 160.dp
private val MarkerRadius = 10.dp

/** Test tag of the picker's image, present once the photo has been decoded. */
internal const val BackgroundFocusImageTag = "background-focus-image"

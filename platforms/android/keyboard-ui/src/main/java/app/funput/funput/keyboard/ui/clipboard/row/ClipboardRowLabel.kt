package app.funput.funput.keyboard.ui.clipboard.row

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.R
import app.funput.funput.keyboard.ui.clipboard.ClipboardRowText
import app.funput.funput.keyboard.ui.clipboard.ClipboardTimeStrings
import app.funput.funput.keyboard.ui.clipboard.KeyboardClipboardEntry
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import java.time.Instant

/** The text of a history card: up to two lines of [preview] above how long ago it was copied. */
@Composable
internal fun ClipboardRowLabel(
    entry: KeyboardClipboardEntry,
    preview: String,
    now: Instant,
    palette: KeyboardPanelPalette,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val strings = ClipboardTimeStrings(
        stringResource(R.string.clipboard_just_now),
        { resources.getString(R.string.clipboard_minutes_ago, it) },
        { resources.getString(R.string.clipboard_hours_ago, it) },
        { resources.getString(R.string.clipboard_days_ago, it) },
    )
    Column(modifier.padding(start = 12.dp, top = 7.dp, bottom = 7.dp)) {
        BasicText(
            preview,
            style = TextStyle(Color(palette.readableOn(palette.cardSurface, palette.label)), 14.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(
            ClipboardRowText.relativeTime(entry.capturedAt, now, strings),
            style = TextStyle(Color(palette.readableOn(palette.cardSurface, palette.secondaryLabel)), 11.sp),
        )
    }
}

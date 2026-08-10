package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/**
 * The mark, oversized and nearly invisible, behind the setup card.
 *
 * A flat panel of colour is the difference between a card and a printed one. The reference this
 * was measured against puts a dragon behind its price and a coin behind its list; Funput has its
 * own mark and no need to borrow a motif.
 */
@Composable
internal fun BoxScope.SetupWatermark() {
    Image(
        painter = painterResource(R.drawable.ic_funput_monochrome),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = 40.dp)
            .size(210.dp)
            .alpha(0.06f),
    )
}

/** Wraps a card so the watermark sits behind its content and never outside its corners. */
@Composable
internal fun WatermarkedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.clipToBounds()) {
        SetupWatermark()
        content()
    }
}

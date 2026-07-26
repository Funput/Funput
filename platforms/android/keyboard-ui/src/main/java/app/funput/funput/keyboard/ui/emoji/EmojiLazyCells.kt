package app.funput.funput.keyboard.ui.emoji

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun EmojiCell(item: EmojiItem, modifier: Modifier = Modifier, selected: (EmojiItem) -> Unit) {
    Box(
        modifier = modifier
            .width(44.dp)
            .height(44.dp)
            .semantics { contentDescription = item.accessibilityLabel }
            .clickable { selected(item) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(item.glyph, style = TextStyle(fontSize = 28.sp))
    }
}

@Composable
internal fun EmojiEmptyState(label: String, color: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(label, style = TextStyle(color = androidx.compose.ui.graphics.Color(color), fontSize = 15.sp))
    }
}

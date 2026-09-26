package app.funput.funput.glassspike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Grouped = Color(0xFFF2F2F7)
private val Accent = Color(0xFFD46B08)
private val Swatches = listOf(
    Color(0xFFFF9500), Color(0xFFFF2D55), Color(0xFFAF52DE), Color(0xFF007AFF), Color(0xFF34C759),
)

/**
 * An iOS-style settings list: a large title, grouped cards with coloured icon tiles, and a colour
 * banner every few sections, so the glass above it has something vivid to refract while scrolling.
 */
@Composable
fun SpikeContent(listState: LazyListState, modifier: Modifier = Modifier) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 96.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize().background(Grouped),
    ) {
        item { BasicText("Cài đặt", style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold)) }
        repeat(12) { section ->
            if (section % 3 == 1) item { Banner(section) }
            item { Card(section) }
        }
    }
}

@Composable
private fun Banner(section: Int) {
    val colors = listOf(Swatches[section % 5], Swatches[(section + 2) % 5])
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors)).padding(18.dp),
    ) {
        BasicText(
            "Chủ đề ${section + 1}",
            style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun Card(section: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White)
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        repeat(4) { row ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Swatches[(section + row) % 5]))
                Spacer(Modifier.width(12.dp))
                BasicText("Mục ${section + 1}.${row + 1} — gõ tiếng Việt", style = TextStyle(fontSize = 17.sp))
                Spacer(Modifier.weight(1f))
                BasicText(if (row == 0) "VNI" else "Bật", style = TextStyle(fontSize = 17.sp, color = Accent))
            }
        }
    }
}

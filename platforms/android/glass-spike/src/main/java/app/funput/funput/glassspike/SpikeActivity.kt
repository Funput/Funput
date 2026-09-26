package app.funput.funput.glassspike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Hosts the spike screen in the glass mode passed as the `mode` intent extra. */
class SpikeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mode = GlassMode.parse(intent.getStringExtra("mode"))
        setContent { SpikeScreen(mode) }
    }
}

@Composable
private fun SpikeScreen(mode: GlassMode) {
    val sources = rememberGlassSources(mode)
    val listState = rememberLazyListState()
    with(sources) {
        Box(Modifier.fillMaxSize()) {
            SpikeContent(listState, Modifier.glassSource())
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(0.dp), refract = false)
                    .windowInsetsPadding(WindowInsets.statusBars).height(52.dp),
            ) {
                BasicText(mode.label, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold))
            }
            TabBar(Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun GlassSources.TabBar(modifier: Modifier) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth().height(64.dp).glass(pill),
    ) {
        listOf("Cài đặt", "Giao diện", "Giới thiệu").forEachIndexed { index, label ->
            val selected = index == 0
            BasicText(
                label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color(0xFFD46B08) else Color(0xFF3C3C43),
                ),
                modifier = Modifier.clip(pill)
                    .background(if (selected) Color(0x1FD46B08) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

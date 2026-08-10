package app.funput.funput.keyboard.ui.kaomoji.panel

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.funput.funput.keyboard.ui.emoji.EmojiCategory
import app.funput.funput.keyboard.ui.emoji.EmojiCategoryIcon
import app.funput.funput.keyboard.ui.kaomoji.catalog.KaomojiCategory
import app.funput.funput.keyboard.ui.panel.KeyboardPanelComposeView
import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette

internal class KaomojiBottomBarView(context: Context) : KeyboardPanelComposeView(context) {
    var onLettersRequested: () -> Unit = {}
    var onEmojiRequested: () -> Unit = {}
    var onBackspaceRequested: () -> Unit = {}
    var onCategoryRequested: (KaomojiCategory) -> Unit = {}
    private var palette by mutableStateOf<KeyboardPanelPalette?>(null)
    private var selectedCategory by mutableStateOf(KaomojiCategory.HAPPY)

    init { setContent { Content() } }

    fun updatePalette(value: KeyboardPanelPalette) { palette = value }
    fun setSelected(value: KaomojiCategory) { selectedCategory = value }

    @Composable
    private fun Content() {
        val colors = palette ?: return
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Action("ABC", "Về bàn phím", colors.label, Modifier.width(52.dp), onLettersRequested)
            Action("😀", "Biểu tượng cảm xúc", colors.label, Modifier.width(46.dp), onEmojiRequested)
            LazyRow(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                items(KaomojiCategory.entries, key = KaomojiCategory::name) { category ->
                    val active = category == selectedCategory
                    CategoryAction(category, colors, active)
                }
            }
            Action("⌫", "Xóa", colors.label, Modifier.width(52.dp), onBackspaceRequested)
        }
    }

    @Composable
    private fun CategoryAction(category: KaomojiCategory, colors: KeyboardPanelPalette, active: Boolean) {
        Box(
            Modifier.width(40.dp).height(46.dp)
                .then(if (active) Modifier.background(Color(colors.buttonSurface), RoundedCornerShape(8.dp)) else Modifier)
                .semantics { contentDescription = category.label; selected = active }
                .clickable { onCategoryRequested(category) },
            contentAlignment = Alignment.Center,
        ) {
            val tint = Color(if (active) colors.accent else colors.secondaryLabel)
            if (category == KaomojiCategory.RECENT) {
                EmojiCategoryIcon(EmojiCategory.RECENT, tint)
            } else {
                BasicText(
                    category.symbol,
                    style = TextStyle(color = tint, fontSize = 18.sp, textAlign = TextAlign.Center),
                )
            }
        }
    }

    @Composable
    private fun Action(label: String, description: String, color: Int, modifier: Modifier, clicked: () -> Unit) {
        Box(
            modifier.height(46.dp).semantics { contentDescription = description }.clickable(onClick = clicked),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                label,
                style = TextStyle(Color(color), 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
            )
        }
    }
}

private val KaomojiCategory.symbol: String
    get() = when (this) {
        KaomojiCategory.RECENT -> "◷"
        KaomojiCategory.HAPPY -> "☺"
        KaomojiCategory.LOVE -> "♥"
        KaomojiCategory.SAD -> "☂"
        KaomojiCategory.ANGRY -> "♨"
        KaomojiCategory.SURPRISED -> "!"
        KaomojiCategory.CONFUSED -> "?"
        KaomojiCategory.ACTION -> "↗"
        KaomojiCategory.ANIMAL -> "♧"
        KaomojiCategory.GREETING -> "✋"
    }

package app.funput.funput.keyboard.ui.emoji

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

internal class EmojiBottomBarView(context: Context) : EmojiComposeView(context) {
    var onLettersRequested: () -> Unit = {}
    var onBackspaceRequested: () -> Unit = {}
    var onCategoryRequested: (EmojiCategory) -> Unit = {}
    private var palette by mutableStateOf<EmojiPanelPalette?>(null)
    private var selectedCategory by mutableStateOf(EmojiCategory.SMILEYS_PEOPLE)

    init { setContent { Content() } }

    fun updatePalette(value: EmojiPanelPalette) { palette = value }
    fun setSelected(value: EmojiCategory) { selectedCategory = value }

    @Composable
    private fun Content() {
        val colors = palette ?: return
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Action("ABC", "Về bàn phím", colors.label, Modifier.width(52.dp)) {
                onLettersRequested()
            }
            LazyRow(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                items(EmojiCategory.entries, key = EmojiCategory::name) { category ->
                    val active = category == selectedCategory
                    CategoryAction(
                        category,
                        if (active) colors.accent else colors.secondaryLabel,
                        Modifier
                            .width(44.dp)
                            .then(
                                if (active) Modifier.background(
                                    Color(colors.buttonSurface),
                                    RoundedCornerShape(8.dp),
                                ) else Modifier,
                            ),
                        active = active,
                    ) { onCategoryRequested(category) }
                }
            }
            Action("⌫", "Xóa", colors.label, Modifier.width(52.dp)) {
                onBackspaceRequested()
            }
        }
    }

    @Composable
    private fun CategoryAction(
        category: EmojiCategory,
        color: Int,
        modifier: Modifier,
        active: Boolean,
        clicked: () -> Unit,
    ) {
        Box(
            modifier.height(46.dp)
                .semantics { contentDescription = category.label; selected = active }
                .clickable(onClick = clicked),
            contentAlignment = Alignment.Center,
        ) {
            EmojiCategoryIcon(category, Color(color))
        }
    }

    @Composable
    private fun Action(
        label: String,
        description: String,
        color: Int,
        modifier: Modifier,
        active: Boolean = false,
        clicked: () -> Unit,
    ) {
        Box(
            modifier.height(46.dp)
                .semantics { contentDescription = description; selected = active }
                .clickable(onClick = clicked),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                label,
                style = TextStyle(
                    color = Color(color),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

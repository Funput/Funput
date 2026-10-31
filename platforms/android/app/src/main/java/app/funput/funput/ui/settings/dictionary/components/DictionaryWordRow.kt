package app.funput.funput.ui.settings.dictionary.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryOrigin
import app.funput.funput.ime.suggestions.dictionary.DictionaryWord
import app.funput.funput.ui.kit.gestures.SwipeAction
import app.funput.funput.ui.kit.gestures.SwipeActions
import app.funput.funput.ui.kit.icons.FunputListIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.theme.tint
import app.funput.funput.ui.settings.dictionary.model.detail

/**
 * One word: the word, what the dictionary knows about it, and a thin bar for how often it is
 * used next to the most used word in the list.
 *
 * Swiping and the long-press menu offer the same edits. Blocking applies at once, since the
 * blocked list undoes it; forgetting loses the count, so the screen asks first.
 */
@Composable
internal fun DictionaryWordRow(word: DictionaryWord, highestUses: Int, actions: DictionaryWordActions) {
    val colors = FunputUi.colors
    val blocked = word.origin == DictionaryOrigin.BLOCKED
    val unblock = SwipeAction(stringResource(R.string.dictionary_unblock), colors.accent) { actions.unblock(word) }
    val block = SwipeAction(stringResource(R.string.dictionary_block), colors.tint(FunputTint.ORANGE)) { actions.block(word) }
    val forget = SwipeAction(stringResource(R.string.dictionary_delete), colors.destructive) { actions.forget(word) }
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val menuLabel = stringResource(R.string.dictionary_menu_label)
    Box {
        SwipeActions(
            modifier = Modifier.testTag(dictionaryWordTag(word.text)),
            start = if (blocked) null else block,
            end = if (blocked) unblock else forget,
        ) {
            FunputRow(
                title = word.text,
                summary = word.detail(),
                accessory = { Trailing(word, highestUses) },
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        })
                    }
                    .semantics(mergeDescendants = true) { onLongClick(menuLabel) { menuOpen = true; true } },
            )
        }
        DictionaryWordMenu(word, actions, expanded = menuOpen, onDismiss = { menuOpen = false })
    }
}

@Composable
private fun Trailing(word: DictionaryWord, highestUses: Int) {
    val colors = FunputUi.colors
    when (word.origin) {
        DictionaryOrigin.LEARNED -> UsageBar(if (highestUses > 0) word.uses.toFloat() / highestUses else 0f)
        DictionaryOrigin.ADDED -> BasicText(
            stringResource(R.string.dictionary_badge_added),
            style = FunputUi.typography.caption.copy(color = colors.accent, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clip(FunputUi.shapes.capsule).background(colors.accent.copy(alpha = BadgeFillAlpha))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        DictionaryOrigin.BLOCKED -> Image(
            painterResource(FunputListIcons.Block), null, Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(colors.secondaryLabel),
        )
    }
}

/** How used a word is, as a short capsule in the accent. Never quite empty: every listed word was used. */
@Composable
private fun UsageBar(fraction: Float) {
    val accent = FunputUi.colors.accent
    Box(Modifier.size(width = 44.dp, height = 6.dp).clip(FunputUi.shapes.capsule).background(accent.copy(alpha = BadgeFillAlpha))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(MinUsageFraction, 1f)).background(accent))
    }
}

/** Test tag of a word's row. */
internal fun dictionaryWordTag(word: String) = "dictionary-word-$word"

private const val BadgeFillAlpha = 0.14f
private const val MinUsageFraction = 0.1f

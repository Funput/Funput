package app.funput.funput.keyboard.ui.emoji

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.funput.funput.keyboard.KeyboardSurfaceView
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.keyboard.ui.R
import app.funput.funput.theme.KeyboardTheme

internal class EmojiSearchContentView(context: Context) : EmojiComposeView(context) {
    var onEmojiSelected: (EmojiItem) -> Unit = {}
    var onInput: (String) -> Unit = {}
    var onSpace: () -> Unit = {}
    var onBackspace: () -> Unit = {}
    var onDone: () -> Unit = {}
    var onCancel: () -> Unit = {}
    private var state by mutableStateOf(EmojiSearchState())
    private var items by mutableStateOf(emptyList<EmojiItem>())
    private var palette by mutableStateOf<EmojiPanelPalette?>(null)
    private var theme by mutableStateOf<KeyboardTheme?>(null)
    private var haptics by mutableStateOf(true)
    private var sounds by mutableStateOf(true)
    private val emptyLabel = context.getString(R.string.emoji_search_empty)
    private val keyboard = KeyboardSurfaceView(context).apply {
        layoutOverride = EmojiSearchKeyboardLayout.layout
        suggestionBarEnabled = false
        enterAction = KeyboardEnterAction.Custom("Xong")
        callbacks.onKeyAction = ::handleAction
        callbacks.onEmojiRequested = { onCancel() }
    }

    init { setContent { Content() } }

    fun updateTheme(value: KeyboardTheme) { theme = value }

    fun updateFeedback(haptics: Boolean, sounds: Boolean) {
        this.haptics = haptics
        this.sounds = sounds
    }

    fun render(
        state: EmojiSearchState,
        items: List<EmojiItem>,
        palette: EmojiPanelPalette,
    ) {
        this.state = state
        this.items = items
        this.palette = palette
    }

    @Composable
    private fun Content() {
        val colors = palette ?: return
        val editing = state.mode == EmojiSearchMode.EDITING
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().then(if (editing) Modifier.height(54.dp) else Modifier.weight(1f))) {
                EmojiResultsContent(items, !editing, onEmojiSelected)
                if (state.query.isNotEmpty() && items.isEmpty()) {
                    EmojiEmptyState(emptyLabel, colors.secondaryLabel)
                }
            }
            if (editing) {
                AndroidView(
                    factory = { keyboard },
                    update = {
                        theme?.let { value -> it.keyboardTheme = value }
                        it.isHapticFeedbackEnabled = haptics
                        it.isSoundEffectsEnabled = sounds
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }

    private fun handleAction(action: KeyAction) = when (action) {
        is KeyAction.Input -> onInput(action.text)
        KeyAction.Space -> onSpace()
        KeyAction.Backspace -> onBackspace()
        KeyAction.Enter -> onDone()
        else -> Unit
    }
}

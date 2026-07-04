package app.funput.funput.ui.playground

import androidx.compose.runtime.saveable.listSaver

internal val PlaygroundTextBufferSaver = listSaver<PlaygroundTextBuffer, Any>(
    save = { buffer -> listOf(buffer.text, buffer.cursor) },
    restore = { values ->
        PlaygroundTextBuffer.from(
            text = values[0] as String,
            cursor = values[1] as Int,
        )
    },
)

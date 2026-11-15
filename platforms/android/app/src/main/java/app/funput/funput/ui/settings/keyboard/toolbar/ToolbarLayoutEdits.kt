package app.funput.funput.ui.settings.keyboard.toolbar

import app.funput.funput.ime.settings.layout.ToolbarLayoutPreferences
import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot

/** Pure list edits for the toolbar sides; the section applies the result to the store. */
internal object ToolbarLayoutEdits {
    fun moveUp(preferences: ToolbarLayoutPreferences, side: ToolbarSide, index: Int): ToolbarLayoutPreferences {
        if (index <= 0) return preferences
        return preferences.withSide(side) { slots ->
            slots.toMutableList().also { it.swap(index, index - 1) }
        }
    }

    fun moveDown(preferences: ToolbarLayoutPreferences, side: ToolbarSide, index: Int): ToolbarLayoutPreferences {
        val slots = preferences.slots(side)
        if (index < 0 || index >= slots.lastIndex) return preferences
        return preferences.withSide(side) { list ->
            list.toMutableList().also { it.swap(index, index + 1) }
        }
    }

    fun moveToOtherSide(
        preferences: ToolbarLayoutPreferences,
        side: ToolbarSide,
        index: Int,
    ): ToolbarLayoutPreferences {
        val from = preferences.slots(side).toMutableList()
        if (index !in from.indices) return preferences
        val slot = from.removeAt(index)
        val to = preferences.slots(side.other).toMutableList().also { it += slot }
        return when (side) {
            ToolbarSide.LEADING -> ToolbarLayoutPreferences(from, to)
            ToolbarSide.TRAILING -> ToolbarLayoutPreferences(to, from)
        }
    }

    private fun ToolbarLayoutPreferences.slots(side: ToolbarSide) = when (side) {
        ToolbarSide.LEADING -> leading
        ToolbarSide.TRAILING -> trailing
    }

    private fun ToolbarLayoutPreferences.withSide(
        side: ToolbarSide,
        transform: (List<ToolbarSlot>) -> List<ToolbarSlot>,
    ): ToolbarLayoutPreferences = when (side) {
        ToolbarSide.LEADING -> copy(leading = transform(leading))
        ToolbarSide.TRAILING -> copy(trailing = transform(trailing))
    }

    private fun <T> MutableList<T>.swap(a: Int, b: Int) {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }
}

internal enum class ToolbarSide {
    LEADING,
    TRAILING,
    ;

    val other: ToolbarSide
        get() = if (this == LEADING) TRAILING else LEADING
}

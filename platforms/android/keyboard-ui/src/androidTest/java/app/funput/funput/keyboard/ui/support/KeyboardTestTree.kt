package app.funput.funput.keyboard.ui.support

import android.view.View
import android.view.ViewGroup

/** Finds public view types through placement containers without assuming child indices. */
internal fun ViewGroup.descendants(): Sequence<View> = sequence {
    repeat(childCount) { index ->
        val child = getChildAt(index)
        yield(child)
        if (child is ViewGroup) yieldAll(child.descendants())
    }
}

internal inline fun <reified T : View> ViewGroup.childOfType(): T =
    descendants().filterIsInstance<T>().single()

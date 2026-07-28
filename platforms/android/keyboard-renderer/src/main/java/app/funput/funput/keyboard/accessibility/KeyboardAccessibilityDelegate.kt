package app.funput.funput.keyboard.accessibility

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import kotlin.math.ceil
import kotlin.math.floor

/** Exposes the Canvas keyboard's keys as virtual accessibility buttons. */
internal class KeyboardAccessibilityDelegate(
    private val host: View,
    private val snapshot: () -> KeyboardAccessibilitySnapshot?,
    private val activate: (keyId: String) -> Unit,
    private val activateAlternate: (keyId: String, index: Int) -> Unit,
) : ExploreByTouchHelper(host) {
    init {
        ViewCompat.setAccessibilityDelegate(host, this)
    }

    override fun getVirtualViewAt(x: Float, y: Float): Int =
        snapshot()?.nodeAt(x, y)?.virtualId ?: INVALID_ID

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        snapshot()?.nodes?.mapTo(virtualViewIds) { it.virtualId }
    }

    @Suppress("DEPRECATION")
    override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        node: AccessibilityNodeInfoCompat,
    ) {
        // The virtual id may be stale if the keyboard was rebuilt between
        // getVisibleVirtualViews() and this callback; keep the node valid but empty.
        val key = snapshot()?.node(virtualViewId) ?: run {
            node.contentDescription = ""
            node.setBoundsInParent(EMPTY_BOUNDS)
            return
        }
        node.contentDescription = key.label
        node.className = Button::class.java.name
        node.isClickable = true
        node.isEnabled = host.isEnabled
        node.isSelected = key.selected
        node.setBoundsInParent(key.bounds.toRect())
        node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
        key.alternateActions.forEach { alternate ->
            node.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    alternate.actionId,
                    alternate.label,
                ),
            )
        }
    }

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?,
    ): Boolean {
        if (!host.isEnabled) return false
        val key = snapshot()?.node(virtualViewId) ?: return false
        val alternate = key.alternateActions.firstOrNull { it.actionId == action }
        when {
            action == AccessibilityNodeInfo.ACTION_CLICK -> activate(key.keyId)
            alternate != null -> activateAlternate(key.keyId, alternate.index)
            else -> return false
        }
        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
        return true
    }

    fun dispatchHover(event: MotionEvent): Boolean = dispatchHoverEvent(event)

    private companion object {
        // ExploreByTouchHelper rejects empty bounds, so a stale node gets a 1px box.
        val EMPTY_BOUNDS = Rect(0, 0, 1, 1)
    }
}

private fun app.funput.funput.keyboard.layout.KeyBounds.toRect() = Rect(
    floor(left).toInt(),
    floor(top).toInt(),
    ceil(right).toInt(),
    ceil(bottom).toInt(),
)

package app.funput.funput.keyboard.interaction.gestures

import app.funput.funput.keyboard.interaction.gestures.cursor.SpaceCursorPanTracker
import app.funput.funput.keyboard.model.KeySpec
import kotlin.math.hypot

/** Per-pointer smart-gesture state snapshotted at touch-down. */
internal class SmartGestureSession(
    val initialKey: KeySpec,
    val startX: Float,
    val startY: Float,
    val smartGestures: Boolean,
    private val density: Float,
) {
    var holdArmed = false
    var hasWandered = false
    var trackpad: SpaceCursorPanTracker? = null
    var ratchet: BackspaceWordRatchet? = null
    val claimed: Boolean get() = trackpad != null || ratchet != null
    val translationX get() = lastX - startX
    val translationY get() = lastY - startY
    private var lastX = startX
    private var lastY = startY
    private val slopPx = TapSlopDp * density
    val trackpadActivationPx = TrackpadActivationDp * density
    val stepWidthPx = CaretStepDp * density
    val stepHeightPx = CaretLineStepDp * density
    val ratchetActivationPx = WordActivationDp * density
    val ratchetStepPx = WordStepDp * density

    fun moveTo(x: Float, y: Float) {
        lastX = x
        lastY = y
        if (hasWandered) return
        val dx = x - startX
        val dy = y - startY
        hasWandered = dx * dx + dy * dy > slopPx * slopPx
    }

    /**
     * Distance is measured in any direction, not just sideways: an upward drag is how the caret
     * reaches the line above, and it has no other meaning on the spacebar.
     */
    fun trackpadReady(): Boolean =
        smartGestures && holdArmed && trackpad == null &&
            hypot(translationX, translationY) >= trackpadActivationPx

    companion object {
        const val TapSlopDp = 16f
        const val TrackpadActivationDp = 10f
        const val CaretStepDp = 10f
        const val CaretLineStepDp = 24f
        const val WordActivationDp = 16f
        const val WordStepDp = 40f
    }
}

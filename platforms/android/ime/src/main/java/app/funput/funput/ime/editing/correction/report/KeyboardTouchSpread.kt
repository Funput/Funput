package app.funput.funput.ime.editing.correction.report

import kotlin.math.sqrt

/**
 * How far from key centres this session's touches landed, in key pitches: the number the
 * correction model's finger spread is calibrated against.
 */
internal class KeyboardTouchSpread {
    var count = 0
        private set
    private var sum = 0.0
    private var sumOfSquares = 0.0

    fun record(distance: Float) {
        count += 1
        sum += distance
        sumOfSquares += distance.toDouble() * distance
    }

    val mean: Double get() = if (count == 0) 0.0 else sum / count

    /** The spread of the distances themselves, around their mean. */
    val standardDeviation: Double
        get() = if (count == 0) 0.0 else sqrt((sumOfSquares / count - mean * mean).coerceAtLeast(0.0))

    /** σ per axis of a round 2-D spread whose radial distances these are: √(Σd² / 2n). */
    val perAxisSigma: Double get() = if (count == 0) 0.0 else sqrt(sumOfSquares / (2.0 * count))

    fun reset() {
        count = 0
        sum = 0.0
        sumOfSquares = 0.0
    }
}

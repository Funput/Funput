package app.funput.funput.keyboard.touch

import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.KeyRole
import java.util.Locale
import kotlin.math.sqrt

/**
 * Measures a touch against every key that types a letter or a VNI tone digit, the same way
 * the iOS keyboard does, so the engine sees one kind of evidence from both platforms.
 *
 * Every candidate key is considered, not just the row the finger landed in: a slip onto the
 * row above is exactly the mistake worth repairing.
 */
object KeyTouchEvidenceResolver {
    fun resolve(keyboard: ResolvedKeyboard, x: Float, y: Float, typedKeyId: String): KeyTouchEvidence? {
        if (keyboard.pitchX <= 0f || keyboard.pitchY <= 0f) return null
        var typed: Int? = null
        var typedDistance = 0f
        val nearest = NearestKeys()
        for (row in keyboard.rows) {
            for (key in row) {
                val codePoint = key.codePoint() ?: continue
                val distance = distance(key, x, y, keyboard.pitchX, keyboard.pitchY)
                if (key.spec.id == typedKeyId) {
                    typed = codePoint
                    typedDistance = distance
                } else if (distance <= KeyTouchEvidence.PlausibleDistance) {
                    nearest.offer(codePoint, distance)
                }
            }
        }
        return typed?.let { KeyTouchEvidence(it, typedDistance, nearest.alternates(excluding = it)) }
    }

    private fun distance(key: ResolvedKey, x: Float, y: Float, pitchX: Float, pitchY: Float): Float {
        val dx = (x - key.bounds.centerX) / pitchX
        val dy = (y - key.bounds.centerY) / pitchY
        return sqrt(dx * dx + dy * dy)
    }

    /** The key's lower-case label as one codepoint, for the keys correction can swap in. */
    private fun ResolvedKey.codePoint(): Int? {
        if (spec.role != KeyRole.CHARACTER && spec.role != KeyRole.VNI_MODIFIER) return null
        val label = spec.label.lowercase(Locale.ROOT)
        if (label.isEmpty() || label.codePointCount(0, label.length) != 1) return null
        return label.codePointAt(0)
    }
}

/** The closest few keys seen so far, nearest first, kept by insertion. */
private class NearestKeys {
    private val codePoints = IntArray(KeyTouchEvidence.MaxAlternates)
    private val distances = FloatArray(KeyTouchEvidence.MaxAlternates)
    private var count = 0

    fun offer(codePoint: Int, distance: Float) {
        var slot = count
        while (slot > 0 && distances[slot - 1] > distance) slot--
        if (slot >= KeyTouchEvidence.MaxAlternates) return
        val last = minOf(count, KeyTouchEvidence.MaxAlternates - 1)
        for (index in last downTo slot + 1) {
            codePoints[index] = codePoints[index - 1]
            distances[index] = distances[index - 1]
        }
        codePoints[slot] = codePoint
        distances[slot] = distance
        if (count < KeyTouchEvidence.MaxAlternates) count++
    }

    /** A layout can draw one letter twice; the typed key's own letter is never a neighbour. */
    fun alternates(excluding: Int): List<KeyTouchAlternate> = (0 until count)
        .filter { codePoints[it] != excluding }
        .map { KeyTouchAlternate(codePoints[it], distances[it]) }
}

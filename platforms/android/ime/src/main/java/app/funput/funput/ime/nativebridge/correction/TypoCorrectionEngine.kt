package app.funput.funput.ime.nativebridge.correction

import app.funput.funput.keyboard.touch.KeyTouchEvidence

/**
 * The engine's typo-correction handshake, as the IME uses it. See
 * `docs/features/typo-correction.md` §6 for the order the calls run in.
 *
 * `uses` and `allowed` run parallel to [candidates]: a missing `uses` entry reads as 0 and a
 * missing `allowed` one as permitted, exactly as the engine reads them.
 */
internal interface TypoCorrectionEngine {
    /** Where the finger landed for the key about to be processed. */
    fun setNextKeyTouch(touch: KeyTouchEvidence)

    /** Whether the last key ended a word on a correction waiting for an answer. */
    fun hasPending(): Boolean

    /** The parked correction's candidates, best touch score first. */
    fun candidates(): List<String>

    /** The shown word's length plus the boundary character; 0 when nothing is pending. */
    fun pendingBackspace(): Int

    /** The candidate to apply, or null when the top two are too close to call. */
    fun choose(uses: IntArray, allowed: BooleanArray? = null): Int?

    /** Answers the parked correction: the edit for [index], or null for a decline. */
    fun apply(index: Int?): CorrectionEdit?

    /** Whether the next Backspace would take the last repair back. */
    fun hasUndo(): Boolean

    /** The word as typed, while the undo is armed; empty otherwise. */
    fun undoText(): String

    /** The edit that takes the last repair back, or null when no undo is armed. */
    fun undo(): CorrectionEdit?

    /** How many candidates the word being typed would get at the boundary. */
    fun preview(): Int

    /** Keep the word as typed, and do not offer this spelling again once. */
    fun refuse()

    /** Ranks the candidates in hand without counting anything. */
    fun rank(uses: IntArray, allowed: BooleanArray? = null): CorrectionRanking

    /** The engine's counters since it was created. */
    fun metrics(): CorrectionCounts

    /** An engine with correction compiled out: what every test fake gets for free. */
    object Off : TypoCorrectionEngine {
        override fun setNextKeyTouch(touch: KeyTouchEvidence) = Unit
        override fun hasPending() = false
        override fun candidates() = emptyList<String>()
        override fun pendingBackspace() = 0
        override fun choose(uses: IntArray, allowed: BooleanArray?): Int? = null
        override fun apply(index: Int?): CorrectionEdit? = null
        override fun hasUndo() = false
        override fun undoText() = ""
        override fun undo(): CorrectionEdit? = null
        override fun preview() = 0
        override fun refuse() = Unit
        override fun rank(uses: IntArray, allowed: BooleanArray?) = CorrectionRanking.Refused
        override fun metrics() = CorrectionCounts()
    }
}

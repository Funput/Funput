package app.funput.funput.ime.nativebridge.correction

import app.funput.funput.ime.nativebridge.FunputNative
import app.funput.funput.keyboard.touch.KeyTouchEvidence

/** [TypoCorrectionEngine] over the JNI calls, on the handle [handle] hands out. */
internal class NativeTypoCorrection(private val handle: () -> Long) : TypoCorrectionEngine {
    override fun setNextKeyTouch(touch: KeyTouchEvidence) {
        val alternates = touch.alternates
        fun code(index: Int) = alternates.getOrNull(index)?.codePoint ?: NoNeighbour
        fun distance(index: Int) = alternates.getOrNull(index)?.distance ?: 0f
        FunputNative.nativeSetNextKeyTouch(
            handle(), touch.typed, touch.typedDistance,
            code(0), distance(0), code(1), distance(1), code(2), distance(2),
        )
    }

    override fun hasPending() = FunputNative.nativeHasPendingCorrection(handle())

    override fun candidates(): List<String> =
        FunputNative.nativeCorrectionCandidates(handle())?.toList().orEmpty()

    override fun pendingBackspace() = FunputNative.nativePendingCorrectionBackspace(handle())

    override fun choose(uses: IntArray, allowed: BooleanArray?): Int? =
        FunputNative.nativeChooseCorrection(handle(), uses, allowed).takeIf { it >= 0 }

    override fun apply(index: Int?): CorrectionEdit? =
        CorrectionEdit.decode(FunputNative.nativeApplyCorrection(handle(), index ?: Decline))

    override fun hasUndo() = FunputNative.nativeHasCorrectionUndo(handle())

    override fun undoText(): String = FunputNative.nativeCorrectionUndoText(handle())

    override fun undo(): CorrectionEdit? = CorrectionEdit.decode(FunputNative.nativeUndoCorrection(handle()))

    override fun preview() = FunputNative.nativePreviewCorrection(handle())

    override fun refuse() = FunputNative.nativeRefuseCorrection(handle())

    override fun rank(uses: IntArray, allowed: BooleanArray?): CorrectionRanking =
        CorrectionRanking.decode(FunputNative.nativeRankCorrection(handle(), uses, allowed))

    override fun metrics(): CorrectionCounts =
        CorrectionCounts.decode(FunputNative.nativeCorrectionMetrics(handle()))

    private companion object {
        /** A negative codepoint is how the engine is told a neighbour slot is empty. */
        const val NoNeighbour = -1

        /** A negative index declines the parked correction. */
        const val Decline = -1
    }
}

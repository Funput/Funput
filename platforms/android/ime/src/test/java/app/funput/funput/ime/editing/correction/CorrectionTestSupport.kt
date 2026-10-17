package app.funput.funput.ime.editing.correction

import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.ScriptedEngine
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.nativebridge.correction.CorrectionCounts
import app.funput.funput.ime.nativebridge.correction.CorrectionEdit
import app.funput.funput.ime.nativebridge.correction.CorrectionRanking
import app.funput.funput.ime.nativebridge.correction.CorrectionVerdict
import app.funput.funput.ime.nativebridge.correction.TypoCorrectionEngine
import app.funput.funput.ime.suggestions.correction.CorrectionDictionary
import app.funput.funput.keyboard.touch.KeyTouchEvidence

/** A correction handshake scripted per test, recording the calls it receives in order. */
internal class FakeTypoCorrectionEngine : TypoCorrectionEngine {
    val calls = mutableListOf<String>()
    var pending = false
    var backspace = 0
    var candidates = listOf<String>()
    var chosen: Int? = null
    var ranking = CorrectionRanking.Refused
    var edits = mapOf<Int, CorrectionEdit>()
    var undoEdit: CorrectionEdit? = null
    var previewCount = 0
    val touches = mutableListOf<KeyTouchEvidence>()
    var lastUses: IntArray? = null

    override fun setNextKeyTouch(touch: KeyTouchEvidence) { calls += "touch"; touches += touch }
    override fun hasPending() = pending
    override fun candidates() = candidates
    override fun pendingBackspace() = backspace
    override fun choose(uses: IntArray, allowed: BooleanArray?): Int? = chosen.also { lastUses = uses; calls += "choose" }
    override fun apply(index: Int?): CorrectionEdit? {
        calls += "apply(${index ?: -1})"
        pending = false
        return index?.let(edits::get)?.also { undoEdit = undoEdit ?: CorrectionEdit(it.text.length, "") }
    }
    override fun hasUndo() = undoEdit != null
    override fun undoText() = ""
    override fun undo(): CorrectionEdit? = undoEdit.also { calls += "undo"; undoEdit = null }
    override fun preview() = previewCount.also { calls += "preview" }
    override fun refuse() { calls += "refuse"; previewCount = 0; pending = false }
    override fun rank(uses: IntArray, allowed: BooleanArray?) = ranking.also { lastUses = uses }
    override fun metrics() = CorrectionCounts(applied = 1)

    /** Scripts a dead word ending at a boundary: [shown] + boundary, repaired to [candidates]. */
    fun parkAt(shown: String, vararg candidates: String) {
        pending = true
        backspace = shown.codePointCount(0, shown.length) + 1
        this.candidates = candidates.toList()
        edits = candidates.withIndex().associate { (index, text) -> index to CorrectionEdit(backspace, "$text ") }
    }

    fun ambiguous() {
        ranking = CorrectionRanking(0, 1, CorrectionVerdict.AMBIGUOUS)
    }
}

internal class FakeDictionary(private val known: Set<String> = emptySet()) : CorrectionDictionary {
    val kept = mutableListOf<String>()
    val learned = mutableListOf<String>()
    override fun recognizes(word: String) = word in known || word in kept
    override fun keep(word: String) { kept += word }
    override fun weight(word: String) = if (word == "đường") 4 else 1
    override fun learn(word: String) { learned += word }
}

/** A scripted composition engine with [correction] in place of the inert one. */
internal class CorrectingEngine(
    private val base: ScriptedEngine,
    override val correction: FakeTypoCorrectionEngine,
) : VietnameseEngine by base

/**
 * Defaults to committed rendering, where [app.funput.funput.ime.editing.CommittedEditor]
 * holds the whole document; a composing span never reaches its text.
 */
internal class CorrectionFixture(
    composing: List<String>,
    boundaryOutput: String? = null,
    mode: CompositionRenderMode = CompositionRenderMode.COMMITTED,
    var enabled: Boolean = true,
    var fieldAllows: Boolean = true,
    dictionary: CorrectionDictionary? = FakeDictionary(),
) {
    val correction = FakeTypoCorrectionEngine()
    val engine = CorrectingEngine(ScriptedEngine(ArrayDeque(composing), boundaryOutput), correction)
    val state = TypoCorrection(correction, { enabled }, { fieldAllows }).also { it.dictionary = dictionary }
    val session = AndroidCompositionSession(engine, { it }, state).apply { setRenderMode(mode) }
}

package app.funput.funput.ime.editing.correction

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.ime.nativebridge.NativeVietnameseEngine
import app.funput.funput.ime.nativebridge.PersonalSuggestionNative
import app.funput.funput.ime.suggestions.correction.CorrectionWordCounts
import app.funput.funput.ime.suggestions.correction.CorrectionWordSource
import app.funput.funput.ime.suggestions.correction.NativeCorrectionLexicon
import app.funput.funput.ime.suggestions.correction.TypoKeptWordsStore
import app.funput.funput.ime.suggestions.lexiconInstaller
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** The live document's typo correction and the word source it asks. */
internal class TypoCorrectionParts(val corrections: TypoCorrection, val wordSource: CorrectionWordSource)

/**
 * Builds typo correction for the document engine. It runs only while the switch in
 * settings, which reaches [engine] through its configuration, is on; [policy] is the
 * focused field's, read fresh at every decision.
 */
internal fun createTypoCorrection(
    context: Context,
    engine: NativeVietnameseEngine,
    policy: () -> EditorInfoPolicy,
): TypoCorrectionParts {
    val main = Handler(Looper.getMainLooper())
    val io = Executors.newSingleThreadExecutor { task -> Thread(task, "FunputKeptWords").apply { isDaemon = true } }
    val wordSource = CorrectionWordSource(
        lexicon = NativeCorrectionLexicon { attach -> lexiconInstaller(context).attach(attach) },
        counts = CorrectionWordCounts(PersonalSuggestionNative::nativeSyllableWeight),
        store = TypoKeptWordsStore(context.noBackupFilesDir.resolve(KeptWordsPath), io, Executor(main::post)),
        persists = { policy().allowsPersonalizedLearning },
    )
    val corrections = TypoCorrection(
        engine = engine.correction,
        enabled = { engine.configuration?.typoCorrection == true },
        allowsField = { policy().allowsCorrection },
        allowsLearning = { policy().allowsPersonalizedLearning },
    ).apply { dictionary = wordSource }
    return TypoCorrectionParts(corrections, wordSource)
}

private const val KeptWordsPath = "TypoCorrection/kept-words.txt"

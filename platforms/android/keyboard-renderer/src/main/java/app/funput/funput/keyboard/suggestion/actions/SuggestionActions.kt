package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.interaction.selectionForTarget
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment
import app.funput.funput.keyboard.suggestion.SuggestionChip
import app.funput.funput.keyboard.suggestion.SuggestionChipKind

/**
 * The long-press actions of the suggestion band, as the touch pipeline sees them: a hold on
 * an editable word opens the strip, and a release on one of its slots acts on the word.
 */
internal class SuggestionActions(
    private val suggestions: () -> List<SuggestionChip>,
    schedule: (Runnable, Long) -> Unit,
    cancel: (Runnable) -> Unit,
    touchSlop: Float,
    private val editable: (SuggestionSelection) -> Boolean,
    private val onAction: (SuggestionSelection, SuggestionAction) -> Unit,
    private val onFeedback: () -> Unit,
    capture: (pointerId: Int) -> Unit,
    onChanged: () -> Unit,
) {
    private val strip = SuggestionActionStrip(suggestions, ::emit, onChanged)
    private val hold = SuggestionHold(schedule, cancel, touchSlop, ::isEditable) { pointerId, selection ->
        onFeedback()
        capture(pointerId)
        strip.open(selection)
    }

    /** Only the personal store's own words: a repair or an undo has nothing to forget. */
    fun isEditable(selection: SuggestionSelection): Boolean =
        selection.kind == SuggestionChipKind.SUGGESTION && editable(selection)

    fun segments(keyboard: ResolvedKeyboard): List<SuggestionBandSegment> = strip.segments(keyboard)

    fun targetAt(x: Float, y: Float, keyboard: ResolvedKeyboard?): String? = strip.targetAt(x, y, keyboard)

    fun started(pointerId: Int, targetId: String?, x: Float, y: Float) =
        hold.start(pointerId, targetId?.let(suggestions()::selectionForTarget), x, y)

    fun moved(pointerId: Int, x: Float, y: Float) = hold.move(pointerId, x, y)

    fun isCaptured(pointerId: Int): Boolean = hold.isClaimed(pointerId)

    /** The target the keyboard still acts on: null once the hold or the strip spent the release. */
    fun released(pointerId: Int, targetId: String?): String? =
        targetId.takeUnless { hold.finish(pointerId) || strip.release(targetId) }

    /** TalkBack's custom action, which acts on the word without opening the strip. */
    fun perform(targetId: String, action: SuggestionAction) {
        suggestions().selectionForTarget(targetId)?.takeIf(::isEditable)?.let { emit(it, action) }
    }

    fun cancel() = hold.cancelAll()

    fun close() = strip.close()

    private fun emit(selection: SuggestionSelection, action: SuggestionAction) {
        onFeedback()
        onAction(selection, action)
    }
}

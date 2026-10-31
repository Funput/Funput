package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.interaction.SuggestionTargetIds
import app.funput.funput.keyboard.interaction.gestures.MultiScheduler
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.layout.geometry.resolveGeometry
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionChip

/** A real letters keyboard with its suggestion band, and a [SuggestionActions] driven by hand. */
internal class SuggestionBarFixture(
    var suggestions: List<SuggestionChip>,
    private val editable: (SuggestionSelection) -> Boolean = { true },
) {
    val scheduler = MultiScheduler()
    val actions = mutableListOf<Pair<SuggestionSelection, SuggestionAction>>()
    val captured = mutableListOf<Int>()
    var feedback = 0
    var changes = 0

    val subject = SuggestionActions(
        suggestions = { suggestions },
        schedule = scheduler::schedule,
        cancel = scheduler::cancel,
        touchSlop = TouchSlop,
        editable = editable,
        onAction = { selection, action -> actions += selection to action },
        onFeedback = { feedback++ },
        capture = { captured += it },
        onChanged = { changes++ },
    )

    val band: KeyBounds get() = requireNotNull(keyboard.suggestionBar).suggestionsBounds

    /** A finger resting on candidate [index] until the hold fires. */
    fun hold(pointerId: Int = 1, index: Int = 0) {
        subject.started(pointerId, SuggestionTargetIds.id(index), 10f, 10f)
        scheduler.fire(after = SuggestionHold.HoldDelayMillis)
    }

    /** What the strip puts under a tap at the centre of [targetId]'s slot. */
    fun targetAtCentreOf(targetId: String): String? {
        val segment = subject.segments(keyboard).single { it.targetId == targetId }
        return subject.targetAt(segment.centerX, band.centerY, keyboard)
    }

    companion object {
        const val TouchSlop = 8f

        val keyboard: ResolvedKeyboard by lazy {
            val profile = KeyboardSizingProfile.Normal
            val editor = KeyboardEditorMode.TEXT
            val layout = KeyboardLayoutResolver.resolve(
                inputMethod = KeyboardInputMethod.TELEX,
                mode = KeyboardLayoutMode.LETTERS,
                editorMode = editor,
                systemInputMethodSwitcherVisible = false,
            )
            requireNotNull(layout.resolveGeometry(
                width = KeyboardDimensions.DefaultWidthDp.toInt(),
                height = KeyboardDimensions.recommendedHeightDp(KeyboardInputMethod.TELEX, editor, profile).toInt(),
                density = 1f,
                profile = profile,
            ))
        }
    }
}

import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import os
import PersonalSuggestions
import UIKit

extension KeyboardViewController {
    func installPersonalSuggestions(on surface: KeyboardSurfaceView) {
        personalSuggestionService.onCandidates = {
            [weak self, weak surface] generation, candidates in
            guard let self, activationState.accepts(generation) else { return }
            surface?.updateSuggestions(candidates)
        }
        surface.onSuggestionSelected = { [weak self] candidate in
            self?.handleSuggestionTap(candidate)
        }
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(flushPersonalSuggestions),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
    }

    func configurePersonalSuggestions(
        hasFullAccess: Bool,
        activationGeneration: UInt64
    ) {
        // Opening the word list takes a few milliseconds, so it happens off the way
        // to the first keystroke. Until it is ready the coordinator declines every
        // correction, which is the safe direction.
        if configuration.typoCorrection {
            inputCoordinator.correctionDictionary = correctionWords
            correctionWords.prepare()
            restoreKeptWords(hasFullAccess: hasFullAccess)
        } else {
            inputCoordinator.correctionDictionary = nil
        }
        personalSuggestionService.configure(
            enabled: configuration.personalSuggestionsEnabled,
            hasFullAccess: hasFullAccess,
            resetToken: configuration.personalSuggestionResetToken,
            activationGeneration: activationGeneration
        )
        publishPersonalSuggestionUpdate()
    }

    func publishPersonalSuggestionUpdate() {
        let state = inputCoordinator.state
        let canQuery = configuration.personalSuggestionsEnabled
            && displayedSurface == .funput
            && state.layoutMode == .letters
            && state.editorMode.supportsVietnameseComposition
        let update = inputCoordinator.takePersonalSuggestionUpdate()
        // The correction prior learns every word the user finishes, whether or not
        // personal suggestions are on: it lives in memory and never leaves it.
        if configuration.typoCorrection, let token = update.completedToken {
            correctionWords.learn(token)
        }
        personalSuggestionService.update(
            update,
            canQuery: canQuery,
            shift: state.shiftState,
            correction: inputCoordinator.correctionPreview
        )
    }

    func clearPersonalSuggestions() {
        personalSuggestionService.clear()
        keyboardView.updateSuggestions([])
    }

    func acceptPersonalSuggestion(_ candidate: KeyboardSuggestionCandidate) {
        os_signpost(
            .event,
            log: PersonalSuggestionSignposts.log,
            name: "SuggestionAccept",
            "generation=%{public}llu",
            candidate.generation
        )
        guard let (prefix, text) = personalSuggestionService.acceptance(for: candidate) else {
            clearPersonalSuggestions()
            return
        }
        guard let effects = inputCoordinator.acceptSuggestion(
            text,
            replacing: prefix,
            writer: makeDocumentWriter()
        ) else {
            clearPersonalSuggestions()
            return
        }
        applyPostCommitEffects(effects)
    }

    @objc func flushPersonalSuggestions() {
        personalSuggestionService.flush()
    }
}

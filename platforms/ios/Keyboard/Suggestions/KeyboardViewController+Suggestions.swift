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
            self?.acceptPersonalSuggestion(candidate)
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
            && state.language == .vietnamese
            && state.layoutMode == .letters
            && state.editorMode.supportsVietnameseComposition
        personalSuggestionService.update(
            inputCoordinator.takePersonalSuggestionUpdate(),
            canQuery: canQuery,
            capitalized: state.shiftState != .lowercase
        )
    }

    func clearPersonalSuggestions() {
        personalSuggestionService.clear()
        keyboardView.updateSuggestions([])
    }

    private func acceptPersonalSuggestion(_ candidate: KeyboardSuggestionCandidate) {
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

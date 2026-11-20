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
        ownedSuggestionService?.onCandidates = {
            [weak self, weak surface] generation, candidates in
            guard let self, activationState.accepts(generation) else { return }
            surface?.updateSuggestions(candidates)
        }
        surface.onSuggestionSelected = { [weak self] candidate in
            self?.handleSuggestionTap(candidate)
        }
    }

    /// Once per controller, not once per surface: the surface is bound to every
    /// controller that comes to hold it.
    func observeHostBackgrounding() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(flushPersonalSuggestions),
            name: .NSExtensionHostDidEnterBackground,
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
        ownedSuggestionService?.configure(
            enabled: configuration.personalSuggestionsEnabled,
            hasFullAccess: hasFullAccess,
            resetToken: configuration.personalSuggestionResetToken,
            activationGeneration: activationGeneration
        )
        configureSuggestionMenu(hasFullAccess: hasFullAccess)
        publishPersonalSuggestionUpdate()
    }

    /// The shipped Vietnamese data follows the composer from the coordinator's one writer.
    /// Subscribing hands over the current value, which is why this runs when the
    /// surface is bound: the service is shared, and has to hear this controller's
    /// language the moment it becomes the keyboard, and not before.
    func followSeedLanguage() {
        inputCoordinator.onSeedLanguageChange = { [weak self] language in
            self?.ownedSuggestionService?.setSeedLanguage(language)
        }
    }

    func publishPersonalSuggestionUpdate() {
        guard let service = ownedSuggestionService else { return }
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
        service.update(
            update,
            canQuery: canQuery,
            shift: state.shiftState,
            correction: inputCoordinator.correctionPreview
        )
    }

    func clearPersonalSuggestions() {
        ownedSuggestionService?.clear()
        ownedSurface?.updateSuggestions([])
    }

    func acceptPersonalSuggestion(_ candidate: KeyboardSuggestionCandidate) {
        os_signpost(
            .event,
            log: PersonalSuggestionSignposts.log,
            name: "SuggestionAccept",
            "generation=%{public}llu",
            candidate.generation
        )
        guard let (prefix, text) = ownedSuggestionService?.acceptance(for: candidate) else {
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

    /// Not gated on holding the surface: a flush only writes what was learned, and a
    /// controller that has just lost the keyboard is exactly who should ask for it.
    @objc func flushPersonalSuggestions() {
        runtime.personalSuggestionService.flush()
    }
}

import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import FunputShared
import os
import UIKit

extension KeyboardViewController {
    func installPersonalSuggestions() {
        personalSuggestionService.onCandidates = { [weak self] candidates in
            self?.keyboardView.updateSuggestions(candidates)
        }
        keyboardView.onSuggestionSelected = { [weak self] candidate in
            self?.acceptPersonalSuggestion(candidate)
        }
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(flushPersonalSuggestions),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
    }

    func configurePersonalSuggestions() {
        personalSuggestionService.configure(
            enabled: configuration.personalSuggestionsEnabled,
            hasFullAccess: hasFullAccess,
            resetToken: configuration.personalSuggestionResetToken
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
            canQuery: canQuery
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
        let document = TextDocumentProxyAdapter(proxy: textDocumentProxy)
        guard inputCoordinator.acceptSuggestion(text, replacing: prefix, document: document) else {
            clearPersonalSuggestions()
            return
        }
        publishPersonalSuggestionUpdate()
    }

    @objc func flushPersonalSuggestions() {
        personalSuggestionService.flush()
    }
}

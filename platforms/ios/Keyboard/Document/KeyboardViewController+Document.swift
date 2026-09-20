import KeyboardInput
import PersonalSuggestions
import UIKit

extension KeyboardViewController {
    func makeDocumentWriter() -> KeyboardDocumentWriter {
        KeyboardDocumentWriter(proxy: textDocumentProxy)
    }

    func applyPostCommitEffects(_ effects: KeyboardPostCommitEffects) {
        if effects.suggestionsChanged {
            publishPersonalSuggestionUpdate()
        } else {
            // Shift moved no text, so the words on the bar still stand — only their
            // case does not. `update` would drop this: its dedupe compares prefixes.
            personalSuggestionService.recase(shift: inputCoordinator.state.shiftState)
        }
        if effects.presentationChanged {
            updateInputPresentation()
        }
    }

    func synchronizeInputDocument(event: KeyboardDocumentEvent) {
        let effects = inputCoordinator.synchronizeDocument(
            makeDocumentWriter(),
            event: event
        )
        applyPostCommitEffects(effects)
    }
}

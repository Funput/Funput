import KeyboardInput
import UIKit

extension KeyboardViewController {
    func makeDocumentWriter() -> KeyboardDocumentWriter {
        KeyboardDocumentWriter(proxy: textDocumentProxy)
    }

    func applyPostCommitEffects(_ effects: KeyboardPostCommitEffects) {
        if effects.suggestionsChanged {
            publishPersonalSuggestionUpdate()
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

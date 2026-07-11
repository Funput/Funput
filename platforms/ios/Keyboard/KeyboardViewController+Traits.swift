import KeyboardInput
import UIKit

extension KeyboardViewController {
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        updateTextInputTraits(force: true)
    }

    override func textWillChange(_ textInput: (any UITextInput)?) {
        super.textWillChange(textInput)
        updateTextInputTraits()
    }

    override func textDidChange(_ textInput: (any UITextInput)?) {
        super.textDidChange(textInput)
        updateTextInputTraits()
    }

    func updateTextInputTraits(force: Bool = false) {
        let resolved = TextInputTraitsResolver.resolve(textDocumentProxy)
        guard force || resolved != resolvedTextInputTraits else { return }

        resolvedTextInputTraits = resolved
        inputCoordinator.updateContext(
            editorMode: resolved.editorMode,
            enterAction: resolved.enterAction,
            initialLayoutMode: resolved.initialLayoutMode
        )
        updateInputPresentation()
    }
}

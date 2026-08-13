import FunputShared
import KeyboardInput
import UIKit

extension KeyboardViewController {
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        activateKeyboardForAppearance()
#if DEBUG
        touchDiagnosticsReporter.startIfAvailable(hasFullAccess: hasFullAccess)
#endif
    }

    override func textWillChange(_ textInput: (any UITextInput)?) {
        super.textWillChange(textInput)
        updateTextInputTraits()
    }

    override func textDidChange(_ textInput: (any UITextInput)?) {
        super.textDidChange(textInput)
        updateTextInputTraits()
        synchronizeInputDocument(event: .textChanged)
    }

    override func selectionDidChange(_ textInput: (any UITextInput)?) {
        super.selectionDidChange(textInput)
        synchronizeInputDocument(event: .selectionChanged)
    }

    func updateTextInputTraits() {
        guard applyTextInputTraits() else { return }
        updateInputPresentation()
        // Guarded above, so this only runs when the focused field actually changed —
        // which is exactly when the answer can flip, e.g. moving into a password box.
        refreshClipboardOffer()
    }

    @discardableResult
    func applyTextInputTraits(force: Bool = false) -> Bool {
        let resolved = makeDocumentWriter().inputContext
        guard force || resolved != resolvedTextInputTraits else { return false }

        resolvedTextInputTraits = resolved
        inputCoordinator.updateContext(resolved)
        return true
    }

}

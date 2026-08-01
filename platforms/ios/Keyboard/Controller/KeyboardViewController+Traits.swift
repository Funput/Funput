import FunputShared
import KeyboardInput
import UIKit

extension KeyboardViewController {
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if hasFullAccess { accessStateStore.recordFullAccess() }
        reloadConfiguration()
        updateTextInputTraits(force: true)
        synchronizeInputDocument(event: .activated)
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

    func updateTextInputTraits(force: Bool = false) {
        let resolved = makeDocumentWriter().inputContext
        guard force || resolved != resolvedTextInputTraits else { return }

        resolvedTextInputTraits = resolved
        inputCoordinator.updateContext(resolved)
        updateInputPresentation()
    }

}

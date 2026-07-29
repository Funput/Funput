import FunputShared
import KeyboardInput
import UIKit

extension KeyboardViewController {
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        if hasFullAccess { accessStateStore.recordFullAccess() }
        reloadConfiguration()
        updateTextInputTraits(force: true)
        synchronizeDocument(event: .activated)
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
        synchronizeDocument(event: .textChanged)
    }

    override func selectionDidChange(_ textInput: (any UITextInput)?) {
        super.selectionDidChange(textInput)
        synchronizeDocument(event: .selectionChanged)
    }

    func updateTextInputTraits(force: Bool = false) {
        let resolved = TextInputTraitsResolver.resolve(textDocumentProxy)
        guard force || resolved != resolvedTextInputTraits else { return }

        resolvedTextInputTraits = resolved
        inputCoordinator.updateContext(resolved)
        updateInputPresentation()
    }

    private func synchronizeDocument(event: KeyboardDocumentEvent) {
        let previousState = inputCoordinator.state
        let document = TextDocumentProxyAdapter(proxy: textDocumentProxy)
        inputCoordinator.synchronizeDocument(document, event: event)
        publishPersonalSuggestionUpdate()
        if inputCoordinator.state != previousState {
            updateInputPresentation()
        }
    }
}
